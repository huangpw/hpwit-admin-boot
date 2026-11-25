package com.hpwit.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hpwit.system.mapper.SysFileMapper;
import com.hpwit.system.model.entity.SysFile;
import com.hpwit.system.service.SysFileService;
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
