package com.nova.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nova.system.mapper.SysFileMapper;
import com.nova.system.model.entity.SysFile;
import com.nova.system.service.SysFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author huangpw
 * @Date 2026/2/27 17:52
 */
@Service
@Slf4j
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {
}
