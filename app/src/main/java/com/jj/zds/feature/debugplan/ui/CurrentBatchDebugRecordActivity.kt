package com.jj.zds.feature.debugplan.ui

import android.os.Bundle
import android.view.LayoutInflater
import com.jj.ui.base.JiangToolbarActivity
import com.jj.zds.databinding.ActivityCurrentBatchDebugRecordBinding

class CurrentBatchDebugRecordActivity : JiangToolbarActivity<ActivityCurrentBatchDebugRecordBinding>() {

    override fun createViewBinding(inflater: LayoutInflater): ActivityCurrentBatchDebugRecordBinding {
        return ActivityCurrentBatchDebugRecordBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) = Unit

    override fun toolbarTitle(): CharSequence = "当前批次调试记录"

    override fun toolbarSubtitle(): CharSequence = "Batch Debug Record"
}
