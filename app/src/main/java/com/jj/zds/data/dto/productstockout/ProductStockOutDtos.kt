package com.jj.zds.data.dto.productstockout

data class ProductStockOutTaskDto(
    val id: String? = null,
    val code: String? = null,
    val projectCode: String? = null,
    val deliveryTime: String? = null,
    val airportCode: String? = null,
    val airportName: String? = null,
    val totalQty: Int? = null,
    val finishedQty: Int? = null,
    val dckCount: Int? = null,
    val ckzCount: Int? = null,
    val ywcCount: Int? = null,
    val yqxCount: Int? = null,
    val dtlList: List<ProductStockOutChildTaskDto>? = null,
    val status: Int? = null,
    val statusLabel: String? = null,
)

data class ProductStockOutChildTaskRequest(
    val taskId: String,
)

data class ProductStockOutDetailRequest(
    val dtlId: String,
)

data class ProductStockOutFinishRequest(
    val dtlId: String,
    val forceFlag: Boolean = false,
    val remark: String? = null,
)

data class ProductStockOutChildTaskDto(
    val id: String? = null,
    val taskId: String? = null,
    val categoryName: String? = null,
    val productName: String? = null,
    val seriesName: String? = null,
    val specification: String? = null,
    val warehouseName: String? = null,
    val warehouseCode: String? = null,
    val warehouseType: String? = null,
    val reqQuantity: Int? = null,
    val deliveryQuantity: Int? = null,
    val transferQuantity: Int? = null,
    val standardQuantity: Int? = null,
    val extraQuantity: Int? = null,
    val isInFinWar: Boolean? = null,
    val status: Int? = null,
    val statusLabel: String? = null,
)

data class ProductStockOutScanRequest(
    val dtlId: String,
    val qrCode: String,
)

/** 一次扫码的处理结果，key 为“已出库”“未入库”“老化NG”等原因。 */
data class ProductStockOutScanResult(
    val groups: Map<String, List<String>>,
)

data class ProductStockOutIdRequest(
    val id: String,
)

data class ProductStockOutDetailDto(
    val id: String? = null,
    val taskId: String? = null,
    val categoryId: String? = null,
    val deliveryQuantity: Int? = null,
    val reqQuantity: Int? = null,
    val categoryName: String? = null,
    val productName: String? = null,
    val status: Int? = null,
    val statusLabel: String? = null,
    val resList: List<ProductStockOutRecordDto>? = null,
)

data class ProductStockOutRecordDto(
    val id: String? = null,
    val taskId: String? = null,
    val dtlId: String? = null,
    val materialId: String? = null,
    val quantity: Int? = null,
    val status: Int? = null,
    val materialCode: String? = null,
    val createTime: String? = null,
    val createByName: String? = null,
    val statusLabel: String? = null,
)
