package cn.unipus.plat.demo.em;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum QuesTypeEnum {

    WORD_ADJUST(2, "单词/音标(自适应年龄段)"),

    SENTENCE_ADJUST(4, "句子(自适应年龄段)"),

    PARAGRRAPH(5, "段落"),

    CHOICE_REC(7, "有限分支识别");

    public static QuesTypeEnum getByQuesType(Integer quesType) {
        for (QuesTypeEnum c : QuesTypeEnum.values()) {
            if (c.quesType.equals(quesType)) {
                return c;
            }
        }
        return null;
    }

    private Integer quesType;

    private String desc;
}
