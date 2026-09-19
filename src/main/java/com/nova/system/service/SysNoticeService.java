package com.nova.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nova.system.model.entity.SysNotice;
import com.nova.system.model.form.SysNoticeForm;
import com.nova.system.model.query.SysNoticePageQuery;
import com.nova.system.model.vo.SysNoticeDetailVo;
import com.nova.system.model.vo.SysNoticePageVo;
import com.nova.system.model.vo.SysUserNoticePageVo;

/**
 * 通知公告服务类
 *
 * @author youlaitech
 * @since 2024-08-27 10:31
 */
public interface SysNoticeService extends IService<SysNotice> {

    /**
     * 通知公告分页列表
     *
     * @return 通知公告分页列表
     */
    IPage<SysNoticePageVo> getNoticePage(SysNoticePageQuery queryParams);

    /**
     * 获取通知公告表单数据
     *
     * @param id 通知公告ID
     * @return 通知公告表单对象
     */
    SysNoticeForm getNoticeFormData(Long id);

    /**
     * 新增通知公告
     *
     * @param formData 通知公告表单对象
     * @return 是否新增成功
     */
    boolean saveNotice(SysNoticeForm formData);

    /**
     * 修改通知公告
     *
     * @param id       通知公告ID
     * @param formData 通知公告表单对象
     * @return 是否修改成功
     */
    boolean updateNotice(Long id, SysNoticeForm formData);

    /**
     * 删除通知公告
     *
     * @param ids 通知公告ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteNotices(String ids);

    /**
     * 发布通知公告
     *
     * @param id 通知公告ID
     * @return 是否发布成功
     */
    boolean publishNotice(Long id);

    /**
     * 撤回通知公告
     *
     * @param id 通知公告ID
     * @return 是否撤回成功
     */
    boolean revokeNotice(Long id);

    /**
     * 阅读获取通知公告详情
     *
     * @param id 通知公告ID
     * @return 通知公告详情
     */
    SysNoticeDetailVo getNoticeDetail(Long id);

    /**
     * 获取我的通知公告分页列表
     *
     * @param queryParams 查询参数
     * @return 通知公告分页列表
     */
    IPage<SysUserNoticePageVo> getMyNoticePage(SysNoticePageQuery queryParams);
}
