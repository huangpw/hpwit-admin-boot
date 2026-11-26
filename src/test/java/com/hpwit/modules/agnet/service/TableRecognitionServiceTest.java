package com.hpwit.modules.agnet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpwit.common.result.ResultCode;
import com.hpwit.core.exception.BusinessException;
import com.hpwit.modules.agnet.model.TableRecognitionDraft.Row;
import com.hpwit.modules.agnet.model.TableRecognitionDraft.TableType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableRecognitionServiceTest {

    private final DeepSeekVisionService vision = mock(DeepSeekVisionService.class);
    private final TableRecognitionService service = new TableRecognitionService(vision);

    @Test
    void preservesTextAndAlignsCellsAndParents() throws Exception {
        modelResult("""
                {"tableType":"TREE","columns":["项目","编号","金额"],"rows":[
                  {"cells":["硬件",null,"¥120.00"],"parentRow":null},
                  {"cells":["转换器","001"],"parentRow":0},
                  {"cells":["软件","002","8.50%"],"parentRow":2},
                  {"cells":["网线","003","¥8.50"],"parentRow":0}
                ]}
                """);

        var result = service.recognize(1L, null);

        assertThat(result.tableType()).isEqualTo(TableType.TREE);
        assertThat(result.columns()).containsExactly("项目", "编号", "金额");
        assertThat(result.rows()).containsExactly(
                new Row(Arrays.asList("硬件", null, "¥120.00"), null),
                new Row(Arrays.asList("转换器", "001", null), 0),
                new Row(List.of("软件", "002", "8.50%"), null),
                new Row(List.of("网线", "003", "¥8.50"), null));
    }

    @Test
    void keepsFlatRowsAtTheSameLevelEvenWithRepeatedLabels() throws Exception {
        modelResult("""
                {"tableType":"FLAT","columns":["类别","编号"],"rows":[
                  {"cells":["硬件","001"],"parentRow":null},
                  {"cells":["硬件","002"],"parentRow":0}
                ]}
                """);

        var result = service.recognize(1L, null);

        assertThat(result.tableType()).isEqualTo(TableType.FLAT);
        assertThat(result.rows()).containsExactly(
                new Row(List.of("硬件", "001"), null), new Row(List.of("硬件", "002"), null));
    }

    @Test
    void rendersFlatWhenNoRecognizedParentIsValid() throws Exception {
        modelResult("""
                {"tableType":"TREE","columns":["类别"],"rows":[
                  {"cells":["硬件"],"parentRow":1},
                  {"cells":["软件"],"parentRow":1}
                ]}
                """);

        assertThat(service.recognize(1L, null).tableType()).isEqualTo(TableType.FLAT);
    }

    @Test
    void reportsWhenTheImageHasNoTable() throws Exception {
        modelResult("{\"columns\":[],\"rows\":[]}");

        assertThatThrownBy(() -> service.recognize(1L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getResultCode()).isEqualTo(ResultCode.INVALID_USER_INPUT));
    }

    @Test
    void rejectsRowsWithMoreCellsThanColumns() throws Exception {
        modelResult("{\"tableType\":\"FLAT\",\"columns\":[\"姓名\"],\"rows\":[{\"cells\":[\"甲\",\"乙\"]}]}");

        assertThatThrownBy(() -> service.recognize(1L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getResultCode()).isEqualTo(ResultCode.INTERNAL_SERVER_ERROR));
    }

    private void modelResult(String json) throws Exception {
        when(vision.recognize(eq(1L), isNull(), anyString(), eq(8192), eq(Duration.ofSeconds(75))))
                .thenReturn(new ObjectMapper().readTree(json));
    }
}
