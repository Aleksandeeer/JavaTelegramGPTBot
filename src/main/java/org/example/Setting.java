package org.example;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum Setting {
    text_curie_001("text-curie-001"),
    text_baggage_001("text-baggage-001"),
    text_ada_001("text-ada-001"),
    text_davinci_001("text-davinci-001"),
    code_davinci_edit_002("code-davinci-edit-002"),
    code_cushman_001("code-cushman-001");

    private Setting(String realName) {
        this.realName = realName;
    }
    public String getRealName() {
        return realName;
    }
    private final String realName;
}
