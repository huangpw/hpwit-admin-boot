package com.nova.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.nova.system.converter.SysDeptConverter;
import com.nova.system.model.entity.SysDept;
import com.nova.system.model.form.SysDeptForm;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SysDeptServiceImplTest {

    @Test
    void savesDepartmentWithStringLoginId() {
        SysDeptServiceImpl service = spy(new SysDeptServiceImpl());
        ReflectionTestUtils.setField(service, "sysDeptConverter",
                Mappers.getMapper(SysDeptConverter.class));
        doReturn(0L).when(service).count(any());

        SysDept parent = new SysDept();
        parent.setId(1L);
        parent.setTreePath("0");
        doReturn(parent).when(service).getById(1L);
        doAnswer(invocation -> {
            SysDept saved = invocation.getArgument(0);
            assertEquals(42L, saved.getCreateBy());
            assertEquals("0,1", saved.getTreePath());
            assertEquals("development", saved.getCode());
            saved.setId(100L);
            return true;
        }).when(service).save(any(SysDept.class));

        SysDeptForm form = new SysDeptForm();
        form.setName("研发部");
        form.setCode("development");
        form.setParentId(1L);
        form.setStatus(1);
        form.setSort(1);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn("42");
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(42L);

            assertEquals(100L, service.saveDept(form));
            verify(service).save(any(SysDept.class));
        }
    }
}
