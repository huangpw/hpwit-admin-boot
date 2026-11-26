package com.hpwit.modules.agnet.model;

import java.util.List;

/**
 * 单张截图中主体表格的结构化数据，列和行均按原图顺序排列。
 *
 * @param tableType 根据截图中的层级结构确定的展示类型
 * @param columns 从左到右的表头。多级表头以「 / 」连接
 * @param rows 从上到下的表格行
 */
public record TableRecognitionDraft(TableType tableType, List<String> columns, List<Row> rows) {

    public enum TableType { FLAT, TREE }

    /**
     * @param cells 按列对齐的单元格。空白或无法辨认的内容用 null 表示
     * @param parentRow 父行在 rows 中的零基下标。无明确父行时为 null
     */
    public record Row(List<String> cells, Integer parentRow) {
    }
}
