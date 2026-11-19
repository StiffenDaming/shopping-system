package com.demo.shopping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.shopping.entity.ChatUnanswered;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ChatUnansweredMapper extends BaseMapper<ChatUnanswered> {

    /**
     * 按问题文本聚合，找出最常被问到但知识库没命中的问题，用于补充 FAQ。
     */
    @Select("SELECT question, COUNT(*) AS cnt, MAX(create_time) AS last_time "
            + "FROM chat_unanswered GROUP BY question ORDER BY cnt DESC, last_time DESC LIMIT #{limit}")
    List<Map<String, Object>> topUnanswered(@Param("limit") int limit);
}
