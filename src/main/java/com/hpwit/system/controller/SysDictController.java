package com.hpwit.system.controller;

import com.hpwit.common.base.Option;
import com.hpwit.common.result.Result;
import com.hpwit.system.model.entity.SysDict;
import com.hpwit.system.model.entity.SysDictItem;
import com.hpwit.system.model.form.SysDictForm;
import com.hpwit.system.model.form.SysDictItemForm;
import com.hpwit.system.model.query.SysDictItemQuery;
import com.hpwit.system.model.query.SysDictQuery;
import com.hpwit.system.service.SysDictItemService;
import com.hpwit.system.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: AlbertHPW
 * @Date: 2025/11/22 20:32
 */
@RestController
@RequestMapping("/v1/dict")
public class SysDictController {

    @Autowired
    private SysDictService sysDictService;

    @Autowired
    private SysDictItemService sysDictItemService;

    /** 获取字典库列表 */
    @Operation(summary = "获取字典库列表")
    @GetMapping("/list")
    public Result<?> getDictList(SysDictQuery queryParams) {
        List<SysDict> list = sysDictService.getDictList(queryParams);
        return Result.success(list);
    }

    /** 新增字典 */
    @Operation(summary = "新增字典")
    @PostMapping
    public Result<?> saveDict(@Valid @RequestBody SysDictForm formData) {
        boolean isSave = sysDictService.saveDict(formData);
        return Result.judge(isSave);
    }

    @Operation(summary = "修改字典")
    @PutMapping("/{id}")
    //@PreAuthorize("@ss.hasPerm('sys:dict:edit')")
    public Result<?> updateDict(@PathVariable Long id, @RequestBody SysDictForm dictForm) {
        boolean status = sysDictService.updateDict(id, dictForm);
        // 发送字典更新通知
        //if (status && dictForm.getDictCode() != null) {
        //    webSocketService.broadcastDictChange(dictForm.getDictCode());
        //}
        return Result.judge(status);
    }

    @Operation(summary = "删除字典")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('sys:dict:delete')")
    public Result<?> deleteDict(@Parameter(description = "字典ID，多个以英文逗号(,)拼接") @PathVariable String ids) {
        // 获取字典编码列表，用于发送删除通知
        List<String> dictCodes = sysDictService.getDictCodesByIds(Arrays.stream(ids.split(",")).toList());

        boolean isDelete = sysDictService.deleteDictByIds(Arrays.stream(ids.split(",")).toList());

        // 发送字典删除通知
        //for (String dictCode : dictCodes) {
        //    webSocketService.broadcastDictChange(dictCode);
        //}

        return Result.judge(isDelete);
    }

    /** 根据Id修改 SysDict 状态 */
    @PutMapping("/status/{id}")
    public Result<?> updateDictStatus(@PathVariable Long id, @RequestParam Boolean status) {
        boolean isUpdate = sysDictService.updateDictStatus(id, status);
        return Result.judge(isUpdate);
    }

    /** 获取字典数据列表 */
    @GetMapping("item/list")
    public Result<?> getDictItemList(SysDictItemQuery queryParams) {
        List<SysDictItem> list = sysDictItemService.getDictItemList(queryParams);
        return Result.success(list);
    }

    /** 根据分类编码获取字典数据列表 */
    @GetMapping("/item/list/{dictCode}")
    public Result<?> getDictItemListByCode(@PathVariable String dictCode, @RequestParam(required = false) Boolean status) {
        List<SysDictItem> list = sysDictItemService.getDictItemListByCode(dictCode, status);
        return Result.success(list);
    }

    /**
     * 根据分类编码获取下拉选项列表(走 Redis 缓存)。
     *
     * <p>仅返回 status=1 的启用项，按 sort 升序，返回字段已映射为下拉框所需的 value/label 结构。
     * 缓存策略：Cache-Aside + 写时主动失效 + 版本号变更信号 + TTL 1h 兜底，
     * 任意写操作（新增/修改/删除/启停）都会自动失效该 dictCode 的缓存。</p>
     *
     * <p><b>监测变化</b>：可通过 {@code GET system:dict:version:dictCode} 查看版本号是否变化；
     * 或使用 Redis {@code MONITOR}/{@code SUBSCRIBE __keyevent@*__:string} 监听版本号 key。</p>
     */
    @Operation(summary = "获取字典项下拉选项(走 Redis 缓存)")
    @GetMapping("/options")
    public Result<List<Option<String>>> getDictOptions(
            @Parameter(description = "字典编码", example = "user_status") @RequestParam String dictCode) {
        List<Option<String>> options = sysDictItemService.getDictOptions(dictCode);
        return Result.success(options);
    }

    /**
     * 主动刷新指定字典编码的缓存。
     * <p>适用场景：数据修复后强制刷新 / 接入外部数据源变更通知 / 管理后台"清除缓存"按钮。
     * 调用后会删除 options 缓存并把版本号 +1，下一次 {@code /options} 请求会重新从 DB 加载。</p>
     */
    @Operation(summary = "刷新指定字典编码的缓存(管理端用)")
    @DeleteMapping("/options/refresh/{dictCode}")
    public Result<?> refreshDictCache(@PathVariable String dictCode) {
        sysDictItemService.refreshDictCache(dictCode);
        return Result.success();
    }

    /**
     * 清空所有字典项的 options 缓存与版本号(慎用,会一次性扫所有 key)。
     */
    @Operation(summary = "清空所有字典项缓存(慎用)")
    @DeleteMapping("/options/refresh")
    public Result<?> refreshAllDictCache() {
        sysDictItemService.refreshAllDictCache();
        return Result.success();
    }

    /** 根据Id修改 SysDictItem 状态 */
    @PutMapping("/item/status/{id}")
    public Result<?> updateDictItemStatus(@PathVariable Long id, @RequestParam Boolean status) {
        Boolean isUpdate = sysDictItemService.updateDictItemStatus(id, status);
        return  Result.success(isUpdate);
    }

    @Operation(summary = "新增字典数据")
    @PostMapping("/{dictCode}/item")
    public Result<?> saveDictItem(@PathVariable String dictCode, @Valid @RequestBody SysDictItemForm formData) {
        formData.setDictCode(dictCode);
        boolean result = sysDictItemService.saveDictItem(formData);

        // 发送字典更新通知
        //if (result) {
        //    webSocketService.broadcastDictChange(dictCode);
        //}
        return Result.judge(result);
    }

    @Operation(summary = "字典项表单数据")
    @GetMapping("/{dictCode}/item/{itemId}/form")
    public Result<SysDictItemForm> getDictItemForm(
            @PathVariable String dictCode,
            @Parameter(description = "字典项ID") @PathVariable Long itemId
    ) {
        SysDictItemForm formData = sysDictItemService.getDictItemForm(itemId);
        return Result.success(formData);
    }

    @Operation(summary = "修改字典项")
    @PutMapping("/{dictCode}/item/{itemId}")
    //@PreAuthorize("@ss.hasPerm('sys:dict-item:edit')")
    //@RepeatSubmit
    public Result<?> updateDictItem(
            @PathVariable String dictCode,
            @PathVariable Long itemId,
            @RequestBody SysDictItemForm formData
    ) {
        formData.setId(itemId);
        formData.setDictCode(dictCode);
        boolean status = sysDictItemService.updateDictItem(formData);

        // 发送字典更新通知
        //if (status) {
        //    webSocketService.broadcastDictChange(dictCode);
        //}

        return Result.judge(status);
    }
    @Operation(summary = "删除字典数据")
    @DeleteMapping("/item/{ids}")
    //@PreAuthorize("@ss.hasPerm('sys:dict-item:delete')")
    public Result<?> deleteDictItem(@Parameter(description = "字典项ID，多个以英文逗号(,)拼接") @PathVariable String ids) {
        boolean isDelete = sysDictItemService.deleteByIds(ids);
        return Result.judge(isDelete);
    }
}
