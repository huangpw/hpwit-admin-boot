package com.nova.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nova.common.enums.NoticePublishStatusEnum;
import com.nova.common.enums.NoticeTargetEnum;
import com.nova.core.exception.BusinessException;
import com.nova.system.converter.SysNoticeConverter;
import com.nova.system.mapper.SysNoticeMapper;
import com.nova.system.model.bo.SysNoticeBo;
import com.nova.system.model.dto.NoticeDto;
import com.nova.system.model.entity.SysNotice;
import com.nova.system.model.entity.SysUser;
import com.nova.system.model.entity.SysUserNotice;
import com.nova.system.model.form.SysNoticeForm;
import com.nova.system.model.query.SysNoticePageQuery;
import com.nova.system.model.vo.SysNoticeDetailVo;
import com.nova.system.model.vo.SysNoticePageVo;
import com.nova.system.model.vo.SysUserNoticePageVo;
import com.nova.system.service.SysNoticeService;
import com.nova.system.service.SysUserNoticeService;
import com.nova.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知公告服务实现类
 *
 * @author huangpw
 */
@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {

    private final SysNoticeConverter sysNoticeConverter;
    private final SysUserNoticeService sysUserNoticeService;
    private final SysUserService userService;
    //private final SimpMessagingTemplate messagingTemplate;
    //private final UserOnlineService userOnlineService;

    /**
     * 获取通知公告分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage< SysNoticePageVo >} 通知公告分页列表
     */
    @Override
    public IPage<SysNoticePageVo> getNoticePage(SysNoticePageQuery queryParams) {
        Page<SysNoticeBo> noticePage = this.baseMapper.getNoticePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return sysNoticeConverter.toPageVo(noticePage);
    }

    /**
     * 获取通知公告表单数据
     *
     * @param id 通知公告ID
     * @return {@link SysNoticeForm} 通知公告表单对象
     */
    @Override
    public SysNoticeForm getNoticeFormData(Long id) {
        SysNotice entity = this.getById(id);
        return sysNoticeConverter.toForm(entity);
    }

    /**
     * 新增通知公告
     *
     * @param formData 通知公告表单对象
     * @return {@link Boolean} 是否新增成功
     */
    @Override
    public boolean saveNotice(SysNoticeForm formData) {

        if (NoticeTargetEnum.SPECIFIED.getValue().equals(formData.getTargetType())) {
            List<String> targetUserIdList = formData.getTargetUserIds();
            if (CollectionUtil.isEmpty(targetUserIdList)) {
                throw new BusinessException("推送指定用户不能为空");
            }
        }
        SysNotice entity = sysNoticeConverter.toEntity(formData);
        entity.setCreateBy(StpUtil.getLoginIdAsLong());
        return this.save(entity);
    }

    /**
     * 更新通知公告
     *
     * @param id       通知公告ID
     * @param formData 通知公告表单对象
     * @return {@link Boolean} 是否更新成功
     */
    @Override
    public boolean updateNotice(Long id, SysNoticeForm formData) {
        if (NoticeTargetEnum.SPECIFIED.getValue().equals(formData.getTargetType())) {
            List<String> targetUserIdList = formData.getTargetUserIds();
            if (CollectionUtil.isEmpty(targetUserIdList)) {
                throw new BusinessException("推送指定用户不能为空");
            }
        }

        SysNotice entity = sysNoticeConverter.toEntity(formData);
        return this.updateById(entity);
    }

    /**
     * 删除通知公告
     *
     * @param ids 通知公告ID，多个以英文逗号(,)分割
     * @return {@link Boolean} 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteNotices(String ids) {
        if (StrUtil.isBlank(ids)) {
            throw new BusinessException("删除的通知公告数据为空");
        }

        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        boolean isRemoved = this.removeByIds(idList);
        if (isRemoved) {
            // 删除通知公告的同时，需要删除通知公告对应的用户通知状态
            sysUserNoticeService.remove(new LambdaQueryWrapper<SysUserNotice>().in(SysUserNotice::getNoticeId, idList));
        }
        return isRemoved;
    }

    /**
     * 发布通知公告
     *
     * @param id 通知公告ID
     * @return 是否发布成功
     */
    @Override
    @Transactional
    public boolean publishNotice(Long id) {
        SysNotice notice = this.getById(id);
        if (notice == null) {
            throw new BusinessException("通知公告不存在");
        }

        if (NoticePublishStatusEnum.PUBLISHED.getValue().equals(notice.getPublishStatus())) {
            throw new BusinessException("通知公告已发布");
        }

        Integer targetType = notice.getTargetType();
        String targetUserIds = notice.getTargetUserIds();
        if (NoticeTargetEnum.SPECIFIED.getValue().equals(targetType)
                && StrUtil.isBlank(targetUserIds)) {
            throw new BusinessException("推送指定用户不能为空");
        }

        notice.setPublishStatus(NoticePublishStatusEnum.PUBLISHED.getValue());
        notice.setPublisherId(StpUtil.getLoginIdAsLong());
        notice.setPublishTime(LocalDateTime.now());
        boolean publishResult = this.updateById(notice);

        if (publishResult) {
            // 发布通知公告的同时，删除该通告之前的用户通知数据，因为可能是重新发布
            sysUserNoticeService.remove(
                    new LambdaQueryWrapper<SysUserNotice>().eq(SysUserNotice::getNoticeId, id)
            );

            // 添加新的用户通知数据
            List<String> targetUserIdList = null;
            if (NoticeTargetEnum.SPECIFIED.getValue().equals(targetType)) {
                targetUserIdList = Arrays.asList(targetUserIds.split(","));
            }

            List<SysUser> targetUserList = userService.list(
                    new LambdaQueryWrapper<SysUser>()
                            // 如果是指定用户，则筛选出指定用户
                            .in(
                                    NoticeTargetEnum.SPECIFIED.getValue().equals(targetType),
                                    SysUser::getId,
                                    targetUserIdList
                            )
            );

            List<SysUserNotice> userNoticeList = targetUserList.stream().map(user -> {
                SysUserNotice userNotice = new SysUserNotice();
                userNotice.setNoticeId(id);
                userNotice.setUserId(user.getId());
                userNotice.setIsRead(0);
                return userNotice;
            }).toList();

            if (CollectionUtil.isNotEmpty(userNoticeList)) {
                sysUserNoticeService.saveBatch(userNoticeList);
            }

            Set<String> receivers = targetUserList.stream().map(SysUser::getUsername).collect(Collectors.toSet());

            //Set<String> allOnlineUsers = userOnlineService.getOnlineUsers().stream()
            //  .map(UserOnlineService.UserOnlineDTO::getUsername)
            //  .collect(Collectors.toSet());

            // 找出在线用户的通知接收者
            //Set<String> onlineReceivers = new HashSet<>(CollectionUtil.intersection(receivers, allOnlineUsers));

            NoticeDto noticeDTO = new NoticeDto();
            noticeDTO.setId(id);
            noticeDTO.setTitle(notice.getTitle());
            noticeDTO.setType(notice.getType());
            noticeDTO.setPublishTime(notice.getPublishTime());

            //onlineReceivers.forEach(receiver -> messagingTemplate.convertAndSendToUser(receiver, "/queue/message", noticeDTO));
        }
        return publishResult;
    }

    /**
     * 撤回通知公告
     *
     * @param id 通知公告ID
     * @return 是否撤回成功
     */
    @Override
    @Transactional
    public boolean revokeNotice(Long id) {
        SysNotice notice = this.getById(id);
        if (notice == null) {
            throw new BusinessException("通知公告不存在");
        }

        if (!NoticePublishStatusEnum.PUBLISHED.getValue().equals(notice.getPublishStatus())) {
            throw new BusinessException("通知公告未发布或已撤回");
        }

        notice.setPublishStatus(NoticePublishStatusEnum.REVOKED.getValue());
        notice.setRevokeTime(LocalDateTime.now());
        notice.setUpdateBy(StpUtil.getLoginIdAsLong());

        boolean revokeResult = this.updateById(notice);

        if (revokeResult) {
            // 撤回通知公告的同时，需要删除通知公告对应的用户通知状态
            sysUserNoticeService.remove(new LambdaQueryWrapper<SysUserNotice>()
                    .eq(SysUserNotice::getNoticeId, id)
            );
        }
        return revokeResult;
    }

    /**
     *
     * @param id 通知公告ID
     * @return NoticeDetailVO 通知公告详情
     */
    @Override
    public SysNoticeDetailVo getNoticeDetail(Long id) {
        SysNoticeBo noticeBo = this.baseMapper.getNoticeDetail(id);
        // 更新用户通知公告的阅读状态
        Long userId = StpUtil.getLoginIdAsLong();
        sysUserNoticeService.update(new LambdaUpdateWrapper<SysUserNotice>()
                .eq(SysUserNotice::getNoticeId, id)
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsRead, 0)
                .set(SysUserNotice::getIsRead, 1)
        );
        return sysNoticeConverter.toDetailVO(noticeBo);
    }

    /**
     * 获取当前登录用户的通知公告列表
     *
     * @param queryParams 查询参数
     * @return 通知公告分页列表
     */
    @Override
    public IPage<SysUserNoticePageVo> getMyNoticePage(SysNoticePageQuery queryParams) {
        queryParams.setUserId(StpUtil.getLoginIdAsLong());
        return sysUserNoticeService.getMyNoticePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
    }

}
