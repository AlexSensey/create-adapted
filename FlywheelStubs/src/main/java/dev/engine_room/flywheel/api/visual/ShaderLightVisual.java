package dev.engine_room.flywheel.api.visual;

public interface ShaderLightVisual extends Visual {
    interface SectionCollector {
        default void sections(Object sections) {
        }
    }

    default void setSectionCollector(SectionCollector collector) {
    }
}
