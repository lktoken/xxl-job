package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * index controller
 *
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobinfo/code")
public class JobInfoCodeController {
    private static Logger logger = LoggerFactory.getLogger(JobInfoCodeController.class);

    @Resource
    private JobInfoController jobInfoController;
    @Resource
    private XxlJobService xxlJobService;

    @RequestMapping
    public String index(HttpServletRequest request, Model model,
        @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") int jobGroup) {
        return jobInfoController.index(request, model, jobGroup);
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(value = "start", required = false, defaultValue = "0") int start,
        @RequestParam(value = "length", required = false, defaultValue = "10") int length,
        @RequestParam("jobGroup") int jobGroup, @RequestParam("triggerStatus") int triggerStatus,
        @RequestParam("jobDesc") String jobDesc, @RequestParam("executorHandler") String executorHandler,
        @RequestParam("author") String author, @RequestParam("jobCode") String jobCode) {

        return xxlJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author,
            jobCode);
    }

    @RequestMapping("/add")
    @ResponseBody
    @Transactional
    public ReturnT<String> add(HttpServletRequest request, XxlJobInfo jobInfo) {
        return jobInfoController.add(request, jobInfo);
    }

    @RequestMapping("/update")
    @ResponseBody
    public ReturnT<String> update(HttpServletRequest request, XxlJobInfo jobInfo) {
        return jobInfoController.update(request, jobInfo);
    }

    @RequestMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(@RequestParam("jobCode") String jobCode) {
        XxlJobInfo xxlJobInfo = xxlJobService.loadByCode(jobCode);
        if (xxlJobInfo == null) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        return xxlJobService.remove(xxlJobInfo.getId());
    }

    @RequestMapping("/stop")
    @ResponseBody
    public ReturnT<String> pause(@RequestParam("jobCode") String jobCode) {
        XxlJobInfo xxlJobInfo = xxlJobService.loadByCode(jobCode);
        if (xxlJobInfo == null) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        return xxlJobService.stop(xxlJobInfo.getId());
    }

    @RequestMapping("/start")
    @ResponseBody
    public ReturnT<String> start(@RequestParam("jobCode") String jobCode) {
        XxlJobInfo xxlJobInfo = xxlJobService.loadByCode(jobCode);
        if (xxlJobInfo == null) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        int id = xxlJobInfo.getId();
        return xxlJobService.start(id);
    }

    @RequestMapping("/trigger")
    @ResponseBody
    public ReturnT<String> triggerJob(HttpServletRequest request, @RequestParam("jobCode") String jobCode,
        @RequestParam("executorParam") String executorParam, @RequestParam("addressList") String addressList) {
        XxlJobInfo xxlJobInfo = xxlJobService.loadByCode(jobCode);
        if (xxlJobInfo == null) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        int id = xxlJobInfo.getId();
        // login user
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        // trigger
        return xxlJobService.trigger(loginUser, id, executorParam, addressList);
    }

    @RequestMapping("/nextTriggerTime")
    @ResponseBody
    public ReturnT<List<String>> nextTriggerTime(@RequestParam("scheduleType") String scheduleType,
        @RequestParam("scheduleConf") String scheduleConf) {

        XxlJobInfo paramXxlJobInfo = new XxlJobInfo();
        paramXxlJobInfo.setScheduleType(scheduleType);
        paramXxlJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("nextTriggerTime error. scheduleType = {}, scheduleConf= {}", scheduleType, scheduleConf, e);
            return new ReturnT<List<String>>(ReturnT.FAIL_CODE,
                (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")) + e.getMessage());
        }
        return new ReturnT<List<String>>(result);

    }

}
