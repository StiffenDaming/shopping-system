package com.demo.shopping.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.shopping.dto.ProfileVO;
import com.demo.shopping.entity.User;
import com.demo.shopping.entity.UserProfile;
import com.demo.shopping.mapper.UserMapper;
import com.demo.shopping.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户画像查询服务（管理端展示用）。
 */
@Service
public class ProfileQueryService {

    /** 展示 TOP N 标签 */
    private static final int TOP_N = 5;

    @Resource
    private UserProfileMapper userProfileMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ProfileService profileService;

    @Resource
    private CatalogCache catalogCache;

    /**
     * 画像列表。
     */
    public List<ProfileVO> list() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByAsc(User::getId));
        Map<Long, User> userMap = new LinkedHashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }

        List<UserProfile> profiles = userProfileMapper.selectList(null);
        Map<Long, UserProfile> profileMap = new HashMap<>();
        for (UserProfile profile : profiles) {
            profileMap.put(profile.getUserId(), profile);
        }

        List<ProfileVO> result = new ArrayList<>();
        for (User user : users) {
            result.add(toVO(user, profileMap.get(user.getId())));
        }
        return result;
    }

    /**
     * 单个用户画像详情。
     */
    public ProfileVO detail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        return toVO(user, profile);
    }

    private ProfileVO toVO(User user, UserProfile profile) {
        ProfileVO vo = new ProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        if (profile == null) {
            vo.setActivityLevel("LOW");
            vo.setViewCnt(0);
            vo.setCartCnt(0);
            vo.setOrderCnt(0);
            vo.setTopTags(new ArrayList<>());
            vo.setTopCategories(new ArrayList<>());
            return vo;
        }
        vo.setActivityLevel(profile.getActivityLevel());
        vo.setPricePref(profile.getPricePref());
        vo.setViewCnt(profile.getViewCnt());
        vo.setCartCnt(profile.getCartCnt());
        vo.setOrderCnt(profile.getOrderCnt());
        vo.setUpdateTime(profile.getUpdateTime());

        vo.setTopTags(topOf(profileService.parseVector(profile.getTagVector())));
        vo.setTopCategories(topCategoriesOf(profileService.parseVector(profile.getCategoryVector())));
        return vo;
    }

    private List<ProfileVO.TagWeight> topOf(Map<String, Double> vector) {
        List<ProfileVO.TagWeight> list = new ArrayList<>();
        vector.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_N)
                .forEach(e -> list.add(new ProfileVO.TagWeight(e.getKey(), e.getValue())));
        return list;
    }

    private List<ProfileVO.TagWeight> topCategoriesOf(Map<String, Double> vector) {
        List<ProfileVO.TagWeight> list = new ArrayList<>();
        vector.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_N)
                .forEach(e -> {
                    String key = e.getKey();
                    String name = key;
                    if (key.startsWith("CAT:")) {
                        try {
                            name = catalogCache.categoryName(Long.parseLong(key.substring(4)));
                        } catch (NumberFormatException ignore) {
                            name = key;
                        }
                    }
                    list.add(new ProfileVO.TagWeight(name == null ? key : name, e.getValue()));
                });
        return list;
    }
}
