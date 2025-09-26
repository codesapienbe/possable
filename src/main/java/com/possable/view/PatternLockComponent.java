package com.possable.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.dom.Element;

@Tag("pattern-lock")
@JsModule("./components/pattern-lock.js")
public class PatternLockComponent extends Component {

    public PatternLockComponent(int size) {
        this(size, "rgba(255,255,255,0.6)", 14);
    }

    public PatternLockComponent(int size, String hoverColor, int hoverRadius) {
        // expose pattern property and sizing to the client-side web component
        getElement().setProperty("pattern", "");
        getElement().setAttribute("size", Integer.toString(size));
        getElement().getStyle().set("--pattern-hover-color", hoverColor);
        getElement().getStyle().set("--pattern-hover-radius", Integer.toString(hoverRadius));
    }

    /**
     * Returns the serialized pattern (JSON array of indices) or empty string.
     */
    public String getPattern() {
        String p = getElement().getAttribute("pattern");
        return p == null ? "" : p;
    }

    public void clear() {
        // call the client-side clear method on the custom element
        getElement().callJsFunction("clearPattern");
    }

} 