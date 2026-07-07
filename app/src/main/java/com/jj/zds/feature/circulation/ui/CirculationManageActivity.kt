package com.jj.zds.feature.circulation.ui

import android.os.Bundle
import android.view.LayoutInflater
import com.jj.ui.base.JiangToolbarActivity
import com.jj.zds.databinding.ActivityCirculationManageBinding

class CirculationManageActivity : JiangToolbarActivity<ActivityCirculationManageBinding>() {

    override fun createViewBinding(inflater: LayoutInflater): ActivityCirculationManageBinding {
        return ActivityCirculationManageBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.cardInvalidate.setOnClickListener {
            startActivity(android.content.Intent(this, CirculationDiscardActivity::class.java))
        }
        binding.cardMerge.setOnClickListener { CirculationAdjustActivity.start(this, CirculationAdjustMode.COMBINE) }
        binding.cardAddProduct.setOnClickListener { CirculationAdjustActivity.start(this, CirculationAdjustMode.ADD) }
        binding.cardRemoveProduct.setOnClickListener { CirculationAdjustActivity.start(this, CirculationAdjustMode.REMOVE) }
    }

    override fun toolbarTitle(): CharSequence = "流转单管理"

    override fun toolbarSubtitle(): CharSequence = "异常处置与成品归属调整"

    override fun showBackButton(): Boolean = true

}
