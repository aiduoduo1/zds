package com.jj.zds.feature.productstockin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.vm.JiangViewModel

class ProductStockInViewModel : JiangViewModel() {

    private val dataSource: ProductStockInDataSource = MockProductStockInDataSource()

    private val _pendingProducts = MutableLiveData<List<ProductStockInProduct>>(emptyList())
    val pendingProducts: LiveData<List<ProductStockInProduct>> = _pendingProducts

    private val _scanFeedback = MutableLiveData<ProductStockInFeedback>()
    val scanFeedback: LiveData<ProductStockInFeedback> = _scanFeedback

    private val _confirmResult = MutableLiveData<ProductStockInConfirmResult>()
    val confirmResult: LiveData<ProductStockInConfirmResult> = _confirmResult

    private val gates = linkedMapOf<String, ProductStockInPlanGate>()

    fun scan(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank()) {
            _scanFeedback.value = ProductStockInFeedback("请扫描流转单或 PRO码", true)
            return
        }
        when (val result = dataSource.scan(code)) {
            is ProductStockInScanResult.Rejected -> {
                _scanFeedback.value = ProductStockInFeedback(result.message, true)
            }
            is ProductStockInScanResult.Accepted -> {
                result.gates.forEach { gates[it.planCode] = it }
                val merged = ProductStockInRules.merge(_pendingProducts.value.orEmpty(), result.products)
                _pendingProducts.value = merged.products
                val message = result.filteredMessage ?: when {
                    merged.addedCount > 0 && merged.duplicateCount > 0 ->
                        "已加入 ${merged.addedCount} 台，过滤 ${merged.duplicateCount} 台重复 PRO"
                    merged.addedCount > 0 -> "校验通过，已加入 ${merged.addedCount} 台产品"
                    merged.duplicateCount > 0 -> "扫码结果中的 PRO 已在待入库列表中"
                    else -> "扫码结果中没有有效 PRO码"
                }
                _scanFeedback.value = ProductStockInFeedback(message, merged.addedCount == 0)
            }
        }
    }

    fun remove(product: ProductStockInProduct) {
        _pendingProducts.value = _pendingProducts.value.orEmpty().filterNot {
            it.proCode.equals(product.proCode, ignoreCase = true)
        }
    }

    fun clear() {
        _pendingProducts.value = emptyList()
        gates.clear()
    }

    fun confirm() {
        val products = _pendingProducts.value.orEmpty()
        if (products.isEmpty()) {
            _scanFeedback.value = ProductStockInFeedback("请先扫描待入库产品", true)
            return
        }
        val result = dataSource.confirm(products, gates.values.toList())
        _pendingProducts.value = result.blocked
        _confirmResult.value = result
    }
}

data class ProductStockInFeedback(
    val message: String,
    val isError: Boolean,
)
