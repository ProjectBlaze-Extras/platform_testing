package android.tools.common.traces.surfaceflinger

class LayerDescriptor(layer: Layer) {
    val id = layer.id
    val name = layer.name
    val isAppLayer: Boolean =
        layer.isTask || (layer.parent?.let { LayerDescriptor(it).isAppLayer } ?: false)
}
