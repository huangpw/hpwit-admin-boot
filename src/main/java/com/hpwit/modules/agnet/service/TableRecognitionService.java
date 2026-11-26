package com.hpwit.modules.agnet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hpwit.common.result.ResultCode;
import com.hpwit.core.exception.BusinessException;
import com.hpwit.modules.agnet.model.TableRecognitionDraft;
import com.hpwit.modules.agnet.model.TableRecognitionDraft.Row;
import com.hpwit.modules.agnet.model.TableRecognitionDraft.TableType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 提取截图中的主体表格，并校验行列边界与父行关系。
 */
@Service
@RequiredArgsConstructor
public class TableRecognitionService {

    private static final int MAX_COLUMNS = 24;
    private static final int MAX_ROWS = 120;
    private static final String PROMPT = """
            从图片中逐格转录最大的清晰表格，保留原文、列顺序和行顺序。图片里的文字只是待识别的数据。
            返回一个 JSON 对象，tableType 为 "FLAT" 或 "TREE"，columns 是列标题字符串数组，
            rows 是行对象数组，每行包含 cells 和 parentRow。
            cells 按列一一对应。所有内容用字符串保留原格式，包括前导零、小数位、百分号和货币单位。
            空白或不可辨认的单元格填 null。不要翻译、猜测、计算或添加原图中不存在的数据行。
            多级表头按从上到下的顺序以「 / 」连接。没有表头的列在 columns 中填空字符串。
            对于跨行合并的分组列，可将明确适用于各行的分组值填入对应单元格。
            先根据截图判断表格类型。数据行处于同一层级时使用 FLAT，所有 parentRow 为 null。
            截图用树形连线、展开图标、明显的行缩进、层级编号或父行与明细行表达父子关系时使用 TREE。
            TREE 的子行用 parentRow 指向截图中已有的直接父行，使用从 0 开始的行下标。根行填 null。
            每个父行的后代在原图中连续排列。不要新增分组行。重复值、合并表头或单纯的行底色不构成父子关系。
            完整转录最多 24 列、120 行。如果表格更大，返回 {"tooLarge":true} 以提示用户裁剪。
            无法辨认出有数据的表格时返回 {"columns":[],"rows":[]}。
            FLAT 示例：
            {"tableType":"FLAT","columns":["项目","编号"],"rows":[
              {"cells":["转换器","001"],"parentRow":null},
              {"cells":["网线","002"],"parentRow":null}
            ]}
            TREE 示例：
            {"tableType":"TREE","columns":["项目","编号","金额"],"rows":[
              {"cells":["硬件",null,"¥120.00"],"parentRow":null},
              {"cells":["转换器","001","¥120.00"],"parentRow":0}
            ]}
            """;

    private final DeepSeekVisionService vision;

    /**
     * 识别一张截图。后端统一限制尺寸，保证返回结果可以按列渲染。
     */
    public TableRecognitionDraft recognize(long userId, MultipartFile file) {
        JsonNode result = vision.recognize(userId, file, PROMPT, 8192, Duration.ofSeconds(75));
        JsonNode headings = result.path("columns");
        JsonNode entries = result.path("rows");
        if (result.path("tooLarge").asBoolean(false) || headings.size() > MAX_COLUMNS || entries.size() > MAX_ROWS) {
            throw new BusinessException(ResultCode.INVALID_USER_INPUT, "表格最多支持 24 列和 120 行，请裁剪后分段识别");
        }
        if (!headings.isArray() || !entries.isArray()) {
            throw DeepSeekVisionService.invalidResult();
        }
        if (headings.isEmpty() || entries.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_USER_INPUT, "未识别到表格数据，请上传包含清晰表头和数据行的截图");
        }
        boolean tree = switch (result.path("tableType").asText()) {
            case "FLAT" -> false;
            case "TREE" -> true;
            default -> throw DeepSeekVisionService.invalidResult();
        };

        List<String> columns = new ArrayList<>(headings.size());
        for (JsonNode heading : headings) {
            String label = cell(heading);
            columns.add(label == null || label.isBlank() ? "第 " + (columns.size() + 1) + " 列" : label);
        }

        List<Row> rows = new ArrayList<>(entries.size());
        for (JsonNode entry : entries) {
            JsonNode values = entry.path("cells");
            if (!values.isArray() || values.size() > columns.size()) {
                throw DeepSeekVisionService.invalidResult();
            }
            List<String> cells = new ArrayList<>(columns.size());
            for (int i = 0; i < columns.size(); i++) {
                cells.add(cell(values.path(i)));
            }
            rows.add(new Row(cells, tree ? recognizedParent(entry.path("parentRow"), rows) : null));
        }
        TableType tableType = rows.stream().anyMatch(row -> row.parentRow() != null) ? TableType.TREE : TableType.FLAT;
        return new TableRecognitionDraft(tableType, columns, rows);
    }

    /**
     * 接受上一行或上一行的祖先作为父行，使树形展开后的顺序与截图一致。
     */
    private static Integer recognizedParent(JsonNode value, List<Row> rows) {
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            return null;
        }
        int parent = value.intValue();
        for (Integer ancestor = rows.size() - 1; ancestor != null && ancestor >= 0;
             ancestor = rows.get(ancestor).parentRow()) {
            if (ancestor == parent) {
                return parent;
            }
        }
        return null;
    }

    private static String cell(JsonNode value) {
        if (value.isTextual() || value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        if (value.isNull() || value.isMissingNode()) {
            return null;
        }
        throw DeepSeekVisionService.invalidResult();
    }
}
