package com.hpwit.modules.flowable.service;

import com.hpwit.modules.flowable.model.ApprovalData.Field;
import com.hpwit.modules.flowable.model.ApprovalData.FieldType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class ApprovalBpmn {

    private static final String BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String BPMNDI = "http://www.omg.org/spec/BPMN/20100524/DI";
    private static final String DI = "http://www.omg.org/spec/DD/20100524/DI";
    private static final String DC = "http://www.omg.org/spec/DD/20100524/DC";
    private static final String FLOWABLE = "http://flowable.org/bpmn";
    private static final String XSI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
    private static final Pattern ID = Pattern.compile("[A-Za-z_][A-Za-z0-9_.-]{0,79}");
    private static final Pattern CONDITION = Pattern.compile(
            "\\$\\{\\s*([a-z][A-Za-z0-9_]{0,39})\\s*(>=|<=|==|!=|>|<)\\s*('?)"
                    + "(-?[0-9]{1,12}(?:\\.[0-9]{1,6})?)\\3\\s*}");
    private static final Set<String> TYPES = Set.of("startEvent", "endEvent", "userTask", "exclusiveGateway");
    private static final Set<String> DI_TYPES = Set.of("BPMNDiagram", "BPMNPlane", "BPMNShape", "BPMNEdge",
            "BPMNLabel", "BPMNLabelStyle");
    private static final Map<String, Set<String>> BPMN_CHILDREN = Map.of(
            "definitions", Set.of("process"),
            "process", Set.of("startEvent", "endEvent", "userTask", "exclusiveGateway", "sequenceFlow", "documentation"),
            "startEvent", Set.of("outgoing", "documentation"),
            "endEvent", Set.of("incoming", "documentation"),
            "userTask", Set.of("incoming", "outgoing", "documentation"),
            "exclusiveGateway", Set.of("incoming", "outgoing", "documentation"),
            "sequenceFlow", Set.of("conditionExpression", "documentation"));

    private ApprovalBpmn() {
    }

    static void checkDraft(String xml) {
        parse(xml);
    }

    static String prepare(String xml, String processKey, String name, List<Field> fields,
                          Predicate<String> activeUser, Predicate<String> activeRole) {
        Document document = parse(xml);
        Element definitions = document.getDocumentElement();
        List<Element> processes = children(definitions, "process");
        require(processes.size() == 1, "每个 BPMN 文件必须包含一个流程");
        Element process = processes.get(0);

        Map<String, Element> nodes = new LinkedHashMap<>();
        Map<String, Element> flows = new LinkedHashMap<>();
        Set<String> ids = new HashSet<>();
        for (Element element : children(process, null)) {
            String type = element.getLocalName();
            if ("documentation".equals(type)) {
                continue;
            }
            String id = element.getAttribute("id");
            require(ID.matcher(id).matches() && ids.add(id), "流程节点或连线 ID 无效或重复");
            if (TYPES.contains(type)) {
                nodes.put(id, element);
            } else {
                flows.put(id, element);
            }
        }
        require(nodes.size() <= 60 && flows.size() <= 120, "流程最多包含 60 个节点与 120 条连线");
        var starts = nodes.entrySet().stream().filter(entry -> is(entry.getValue(), "startEvent")).toList();
        require(starts.size() == 1, "流程必须有且仅有一个开始事件");

        Set<String> numericFields = fields.stream()
                .filter(field -> field.required() && field.type() == FieldType.NUMBER)
                .map(Field::key).collect(Collectors.toSet());
        Map<String, List<String>> outgoing = new HashMap<>();
        Map<String, List<String>> incoming = new HashMap<>();
        for (var entry : flows.entrySet()) {
            Element flow = entry.getValue();
            String source = flow.getAttribute("sourceRef");
            String target = flow.getAttribute("targetRef");
            require(nodes.containsKey(source) && nodes.containsKey(target), "连线必须连接有效的流程节点");
            outgoing.computeIfAbsent(source, key -> new ArrayList<>()).add(entry.getKey());
            incoming.computeIfAbsent(target, key -> new ArrayList<>()).add(entry.getKey());
            Element sourceNode = nodes.get(source);
            boolean conditional = is(sourceNode, "exclusiveGateway")
                    && !entry.getKey().equals(sourceNode.getAttribute("default"))
                    && !sourceNode.getAttribute("default").isEmpty();
            var expressions = children(flow, "conditionExpression");
            require(expressions.size() == (conditional ? 1 : 0), "分支条件只能配置在排他网关的非默认连线上");
            if (conditional) {
                var matcher = CONDITION.matcher(expressions.get(0).getTextContent().trim());
                require(matcher.matches() && numericFields.contains(matcher.group(1)),
                        "分支条件必须比较必填数字字段与数值，例如 ${amount >= 1000}");
                // JUEL parses bare decimal literals as floating point. A quoted literal is coerced to BigDecimal.
                expressions.get(0).setTextContent("${%s %s '%s'}"
                        .formatted(matcher.group(1), matcher.group(2), matcher.group(4)));
                expressions.get(0).removeAttributeNS(XSI, "type");
            }
        }

        for (var entry : nodes.entrySet()) {
            Element node = entry.getValue();
            List<String> outs = outgoing.getOrDefault(entry.getKey(), List.of());
            List<String> ins = incoming.getOrDefault(entry.getKey(), List.of());
            verifyReferences(node, "incoming", ins);
            verifyReferences(node, "outgoing", outs);
            switch (node.getLocalName()) {
                case "startEvent" -> require(ins.isEmpty() && outs.size() == 1, "开始事件需要一条出线");
                case "endEvent" -> require(!ins.isEmpty() && outs.isEmpty(), "结束事件需要入线");
                case "userTask" -> {
                    require(!ins.isEmpty() && outs.size() == 1, "审批节点需要入线和一条出线");
                    require(!node.getAttribute("name").isBlank(), "请为每个审批节点填写名称");
                    String assignee = node.getAttributeNS(FLOWABLE, "assignee");
                    String group = node.getAttributeNS(FLOWABLE, "candidateGroups");
                    require(assignee.isEmpty() != group.isEmpty(), "每个审批节点须指定一个审批人或审批角色");
                    if (!assignee.isEmpty()) {
                        require("${applicantId}".equals(assignee)
                                        || (assignee.matches("[1-9][0-9]{0,18}") && activeUser.test(assignee)),
                                "审批人须为有审批中心权限的启用用户或发起人：" + node.getAttribute("name"));
                    } else {
                        require(group.matches("[a-z0-9_-]{1,64}") && activeRole.test(group),
                                "审批角色须已启用且至少有一名具有审批中心权限的成员：" + node.getAttribute("name"));
                    }
                }
                case "exclusiveGateway" -> {
                    require(!ins.isEmpty() && !outs.isEmpty(), "排他网关需要入线和出线");
                    String defaultFlow = node.getAttribute("default");
                    require(outs.size() > 1 ? outs.contains(defaultFlow) : defaultFlow.isEmpty(),
                            "分支网关须指定默认连线，汇聚网关只保留一条普通出线");
                }
                default -> throw new IllegalStateException();
            }
        }
        Set<String> reached = new HashSet<>();
        walk(starts.get(0).getKey(), false, nodes, flows, outgoing, new HashSet<>(), new HashSet<>(), reached);
        require(reached.size() == nodes.size(), "存在从开始事件无法到达的节点");

        String importedId = process.getAttribute("id");
        require(ID.matcher(importedId).matches() && !ids.contains(processKey), "流程 ID 与节点 ID 无效或冲突");
        process.setAttribute("id", processKey);
        process.setAttribute("name", name);
        process.setAttribute("isExecutable", "true");
        for (Element diagram : children(definitions, "BPMNDiagram")) {
            for (Element plane : children(diagram, "BPMNPlane")) {
                require(importedId.equals(plane.getAttribute("bpmnElement")), "BPMN 图必须关联此流程");
                plane.setAttribute("bpmnElement", processKey);
            }
        }
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var writer = new StringWriter();
            factory.newTransformer().transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("生成部署文件失败", e);
        }
    }

    private static void walk(String id, boolean approvedPath, Map<String, Element> nodes, Map<String, Element> flows,
                             Map<String, List<String>> outgoing, Set<String> path, Set<String> visited, Set<String> reached) {
        require(path.add(id), "审批流程须为无环流程");
        Element node = nodes.get(id);
        boolean hasApproval = approvedPath || is(node, "userTask");
        reached.add(id);
        if (visited.add(id + "/" + hasApproval)) {
            require(!is(node, "endEvent") || hasApproval, "每条分支至少需要一个审批节点");
            for (String flow : outgoing.getOrDefault(id, List.of())) {
                walk(flows.get(flow).getAttribute("targetRef"), hasApproval, nodes, flows, outgoing, path, visited, reached);
            }
        }
        path.remove(id);
    }

    private static void verifyReferences(Element node, String reference, List<String> actual) {
        List<Element> refs = children(node, reference);
        Set<String> supplied = refs.stream().map(item -> item.getTextContent().trim()).collect(Collectors.toSet());
        require(refs.isEmpty() || (refs.size() == supplied.size() && supplied.equals(new HashSet<>(actual))),
                "节点的连线引用与实际连线不一致");
    }

    private static Document parse(String xml) {
        require(xml != null && xml.getBytes(StandardCharsets.UTF_8).length <= 524288, "BPMN 文件最大为 512 KB");
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            require(BPMN.equals(root.getNamespaceURI()) && is(root, "definitions"), "请输入标准 BPMN 2.0 XML");
            validateXml(root, 0);
            return document;
        } catch (ApprovalException e) {
            throw e;
        } catch (Exception e) {
            throw new ApprovalException("BPMN XML 格式无效");
        }
    }

    private static void validateXml(Element element, int depth) {
        require(depth <= 12, "BPMN XML 层级过深");
        String namespace = element.getNamespaceURI();
        String type = element.getLocalName();
        boolean bpmn = BPMN.equals(namespace);
        boolean diagram = BPMNDI.equals(namespace) || DI.equals(namespace) || DC.equals(namespace);
        require(bpmn || diagram, "BPMN 不支持元素 " + type);
        Set<String> attributes = bpmn ? bpmnAttributes(type) : Set.of("id", "name", "bpmnElement", "isHorizontal",
                "isExpanded", "isMarkerVisible", "isMessageVisible", "labelStyle", "x", "y", "width", "height",
                "size", "isBold", "isItalic", "isUnderline", "isStrikeThrough");
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            Node attribute = element.getAttributes().item(i);
            String ns = attribute.getNamespaceURI();
            String attr = attribute.getLocalName();
            boolean allowed = XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(ns)
                    || (ns == null && attributes.contains(attr))
                    || (FLOWABLE.equals(ns) && is(element, "userTask") && Set.of("assignee", "candidateGroups").contains(attr))
                    || (XSI.equals(ns) && is(element, "conditionExpression") && "type".equals(attr));
            require(allowed, "BPMN 不支持属性 " + attribute.getNodeName());
            if (bpmn && "name".equals(attr)) {
                require(attribute.getNodeValue().length() <= 150, "BPMN 节点名称最多 150 字");
            }
        }

        for (Element child : children(element, null)) {
            String childNs = child.getNamespaceURI();
            String childType = child.getLocalName();
            boolean allowed = BPMN.equals(childNs) && bpmn && BPMN_CHILDREN.getOrDefault(type, Set.of()).contains(childType)
                    || BPMNDI.equals(childNs) && DI_TYPES.contains(childType)
                    && ((bpmn && is(element, "definitions") && "BPMNDiagram".equals(childType)) || diagram)
                    || diagram && DC.equals(childNs) && Set.of("Bounds", "Font").contains(childType)
                    || diagram && DI.equals(childNs) && "waypoint".equals(childType);
            require(allowed, "BPMN 不支持元素 " + childType);
            validateXml(child, depth + 1);
        }
    }

    private static Set<String> bpmnAttributes(String type) {
        return switch (type) {
            case "definitions" -> Set.of("id", "name", "targetNamespace", "exporter", "exporterVersion",
                    "expressionLanguage", "typeLanguage");
            case "process" -> Set.of("id", "name", "isExecutable", "processType");
            case "exclusiveGateway" -> Set.of("id", "name", "default", "gatewayDirection");
            case "sequenceFlow" -> Set.of("id", "name", "sourceRef", "targetRef");
            case "documentation" -> Set.of("id", "textFormat");
            case "conditionExpression" -> Set.of("id", "language");
            default -> Set.of("id", "name");
        };
    }

    private static boolean is(Element element, String type) {
        return type.equals(element.getLocalName());
    }

    private static List<Element> children(Element element, String type) {
        var elements = new ArrayList<Element>();
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element child && (type == null || is(child, type))) {
                elements.add(child);
            }
        }
        return elements;
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new ApprovalException(message);
        }
    }
}
