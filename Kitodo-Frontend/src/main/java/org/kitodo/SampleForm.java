package org.kitodo;

import javax.faces.bean.SessionScoped;
import javax.inject.Named;

@Named("SampleForm")
@SessionScoped
public class SampleForm {


    public String value = "Test123";

    public SampleForm() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
