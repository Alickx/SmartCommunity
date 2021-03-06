package cn.goroute.smart.task.core.alarm;

import cn.goroute.smart.task.core.model.XxlJobInfo;
import cn.goroute.smart.task.core.model.XxlJobLog;

/**
 * @author xuxueli 2020-01-19
 */
public interface JobAlarm {

    /**
     * job alarm
     *
     * @param info
     * @param jobLog
     * @return
     */
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog);

}
