package com.flowservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 优质蛋白质识别服务
 * 根据 AI 识别的食物列表，匹配规则表识别优质蛋白来源
 * 
 * 数据来源：
 * - 《中国居民膳食指南（2022）》
 * - 中国营养学会"优质蛋白质十佳食物"
 * - 参考链接：http://dg.cnsoc.org
 * 
 * @author Flow Team
 * @version 1.0
 */
@Slf4j
@Service
public class QualityProteinService {

    /**
     * 优质蛋白来源分类枚举
     */
    public enum ProteinCategory {
        EGG("蛋类", "优质蛋白，氨基酸组成与人体需要非常接近"),
        DAIRY("奶类", "优质蛋白，钙的良好来源"),
        FISH("鱼类", "优质蛋白，富含不饱和脂肪酸和DHA"),
        SHRIMP("虾类", "优质蛋白，低脂肪高蛋白"),
        SHELLFISH("贝类", "优质蛋白，必需氨基酸占比高"),
        CRAB("蟹类", "优质蛋白，富含微量元素"),
        POULTRY("禽肉", "优质蛋白，饱和脂肪较少"),
        LEAN_MEAT("畜类瘦肉", "优质蛋白，铁的良好来源"),
        SOY("大豆及制品", "植物优质蛋白，富含大豆异黄酮");

        private final String displayName;
        private final String description;

        ProteinCategory(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 优质蛋白食物规则表
     * Key: 食物关键词（支持 AI 识别结果匹配）
     * Value: 对应的蛋白质分类
     */
    private static final Map<String, ProteinCategory> PROTEIN_RULES = new LinkedHashMap<>();

    static {
        // ========== 蛋类 ==========
        addRule("鸡蛋", ProteinCategory.EGG);
        addRule("蛋", ProteinCategory.EGG);
        addRule("鸭蛋", ProteinCategory.EGG);
        addRule("鹌鹑蛋", ProteinCategory.EGG);
        addRule("鸽子蛋", ProteinCategory.EGG);
        addRule("鹅蛋", ProteinCategory.EGG);
        addRule("蛋羹", ProteinCategory.EGG);
        addRule("煎蛋", ProteinCategory.EGG);
        addRule("炒蛋", ProteinCategory.EGG);
        addRule("蒸蛋", ProteinCategory.EGG);
        addRule("荷包蛋", ProteinCategory.EGG);
        addRule("水煮蛋", ProteinCategory.EGG);
        addRule("茶叶蛋", ProteinCategory.EGG);
        addRule("卤蛋", ProteinCategory.EGG);
        addRule("蛋卷", ProteinCategory.EGG);
        addRule("蛋饼", ProteinCategory.EGG);
        addRule("蛋炒饭", ProteinCategory.EGG);
        addRule("番茄炒蛋", ProteinCategory.EGG);
        addRule("西红柿炒蛋", ProteinCategory.EGG);

        // ========== 奶类 ==========
        addRule("牛奶", ProteinCategory.DAIRY);
        addRule("酸奶", ProteinCategory.DAIRY);
        addRule("奶酪", ProteinCategory.DAIRY);
        addRule("芝士", ProteinCategory.DAIRY);
        addRule("奶", ProteinCategory.DAIRY);
        addRule("乳酪", ProteinCategory.DAIRY);
        addRule("酸乳", ProteinCategory.DAIRY);
        addRule("奶昔", ProteinCategory.DAIRY);
        addRule("奶茶", ProteinCategory.DAIRY);
        addRule("拿铁", ProteinCategory.DAIRY);

        // ========== 鱼类 ==========
        addRule("鱼", ProteinCategory.FISH);
        addRule("三文鱼", ProteinCategory.FISH);
        addRule("鲑鱼", ProteinCategory.FISH);
        addRule("金枪鱼", ProteinCategory.FISH);
        addRule("鳕鱼", ProteinCategory.FISH);
        addRule("鲈鱼", ProteinCategory.FISH);
        addRule("带鱼", ProteinCategory.FISH);
        addRule("黄鱼", ProteinCategory.FISH);
        addRule("大黄鱼", ProteinCategory.FISH);
        addRule("小黄鱼", ProteinCategory.FISH);
        addRule("鲫鱼", ProteinCategory.FISH);
        addRule("鲤鱼", ProteinCategory.FISH);
        addRule("草鱼", ProteinCategory.FISH);
        addRule("鲢鱼", ProteinCategory.FISH);
        addRule("鳙鱼", ProteinCategory.FISH);
        addRule("石斑鱼", ProteinCategory.FISH);
        addRule("鳗鱼", ProteinCategory.FISH);
        addRule("秋刀鱼", ProteinCategory.FISH);
        addRule("沙丁鱼", ProteinCategory.FISH);
        addRule("鲭鱼", ProteinCategory.FISH);
        addRule("青花鱼", ProteinCategory.FISH);
        addRule("罗非鱼", ProteinCategory.FISH);
        addRule("鲶鱼", ProteinCategory.FISH);
        addRule("黑鱼", ProteinCategory.FISH);
        addRule("多宝鱼", ProteinCategory.FISH);
        addRule("比目鱼", ProteinCategory.FISH);
        addRule("鳜鱼", ProteinCategory.FISH);
        addRule("桂鱼", ProteinCategory.FISH);
        addRule("鲳鱼", ProteinCategory.FISH);
        addRule("银鱼", ProteinCategory.FISH);
        addRule("鳝鱼", ProteinCategory.FISH);
        addRule("黄鳝", ProteinCategory.FISH);
        addRule("泥鳅", ProteinCategory.FISH);
        addRule("烤鱼", ProteinCategory.FISH);
        addRule("清蒸鱼", ProteinCategory.FISH);
        addRule("红烧鱼", ProteinCategory.FISH);
        addRule("水煮鱼", ProteinCategory.FISH);
        addRule("酸菜鱼", ProteinCategory.FISH);
        addRule("鱼片", ProteinCategory.FISH);
        addRule("鱼排", ProteinCategory.FISH);
        addRule("鱼肉", ProteinCategory.FISH);
        addRule("刺身", ProteinCategory.FISH);
        addRule("寿司", ProteinCategory.FISH);

        // ========== 虾类 ==========
        addRule("虾", ProteinCategory.SHRIMP);
        addRule("对虾", ProteinCategory.SHRIMP);
        addRule("基围虾", ProteinCategory.SHRIMP);
        addRule("明虾", ProteinCategory.SHRIMP);
        addRule("龙虾", ProteinCategory.SHRIMP);
        addRule("小龙虾", ProteinCategory.SHRIMP);
        addRule("虾仁", ProteinCategory.SHRIMP);
        addRule("白灼虾", ProteinCategory.SHRIMP);
        addRule("油焖大虾", ProteinCategory.SHRIMP);
        addRule("蒜蓉虾", ProteinCategory.SHRIMP);
        addRule("椒盐虾", ProteinCategory.SHRIMP);
        addRule("虾球", ProteinCategory.SHRIMP);
        addRule("虾饺", ProteinCategory.SHRIMP);
        addRule("河虾", ProteinCategory.SHRIMP);
        addRule("海虾", ProteinCategory.SHRIMP);
        addRule("皮皮虾", ProteinCategory.SHRIMP);
        addRule("濑尿虾", ProteinCategory.SHRIMP);
        addRule("北极虾", ProteinCategory.SHRIMP);
        addRule("甜虾", ProteinCategory.SHRIMP);

        // ========== 贝类 ==========
        addRule("扇贝", ProteinCategory.SHELLFISH);
        addRule("生蚝", ProteinCategory.SHELLFISH);
        addRule("牡蛎", ProteinCategory.SHELLFISH);
        addRule("蛤蜊", ProteinCategory.SHELLFISH);
        addRule("花蛤", ProteinCategory.SHELLFISH);
        addRule("花甲", ProteinCategory.SHELLFISH);
        addRule("蛏子", ProteinCategory.SHELLFISH);
        addRule("蚬子", ProteinCategory.SHELLFISH);
        addRule("贻贝", ProteinCategory.SHELLFISH);
        addRule("青口", ProteinCategory.SHELLFISH);
        addRule("淡菜", ProteinCategory.SHELLFISH);
        addRule("鲍鱼", ProteinCategory.SHELLFISH);
        addRule("海螺", ProteinCategory.SHELLFISH);
        addRule("田螺", ProteinCategory.SHELLFISH);
        addRule("蜗牛", ProteinCategory.SHELLFISH);
        addRule("象拔蚌", ProteinCategory.SHELLFISH);
        addRule("蚌", ProteinCategory.SHELLFISH);
        addRule("贝", ProteinCategory.SHELLFISH);
        addRule("海瓜子", ProteinCategory.SHELLFISH);

        // ========== 蟹类 ==========
        addRule("螃蟹", ProteinCategory.CRAB);
        addRule("蟹", ProteinCategory.CRAB);
        addRule("大闸蟹", ProteinCategory.CRAB);
        addRule("梭子蟹", ProteinCategory.CRAB);
        addRule("帝王蟹", ProteinCategory.CRAB);
        addRule("面包蟹", ProteinCategory.CRAB);
        addRule("青蟹", ProteinCategory.CRAB);
        addRule("河蟹", ProteinCategory.CRAB);
        addRule("海蟹", ProteinCategory.CRAB);
        addRule("蟹黄", ProteinCategory.CRAB);
        addRule("蟹肉", ProteinCategory.CRAB);
        addRule("蟹粉", ProteinCategory.CRAB);

        // ========== 禽肉 ==========
        addRule("鸡肉", ProteinCategory.POULTRY);
        addRule("鸡", ProteinCategory.POULTRY);
        addRule("鸡胸肉", ProteinCategory.POULTRY);
        addRule("鸡腿", ProteinCategory.POULTRY);
        addRule("鸡翅", ProteinCategory.POULTRY);
        addRule("鸡排", ProteinCategory.POULTRY);
        addRule("白切鸡", ProteinCategory.POULTRY);
        addRule("烧鸡", ProteinCategory.POULTRY);
        addRule("烤鸡", ProteinCategory.POULTRY);
        addRule("炸鸡", ProteinCategory.POULTRY);
        addRule("宫保鸡丁", ProteinCategory.POULTRY);
        addRule("辣子鸡", ProteinCategory.POULTRY);
        addRule("黄焖鸡", ProteinCategory.POULTRY);
        addRule("口水鸡", ProteinCategory.POULTRY);
        addRule("鸡丝", ProteinCategory.POULTRY);
        addRule("鸡块", ProteinCategory.POULTRY);
        addRule("鸡柳", ProteinCategory.POULTRY);
        addRule("鸡米花", ProteinCategory.POULTRY);
        addRule("鸭肉", ProteinCategory.POULTRY);
        addRule("鸭", ProteinCategory.POULTRY);
        addRule("烤鸭", ProteinCategory.POULTRY);
        addRule("盐水鸭", ProteinCategory.POULTRY);
        addRule("啤酒鸭", ProteinCategory.POULTRY);
        addRule("鸭腿", ProteinCategory.POULTRY);
        addRule("鹅肉", ProteinCategory.POULTRY);
        addRule("烧鹅", ProteinCategory.POULTRY);
        addRule("鹅", ProteinCategory.POULTRY);
        addRule("火鸡", ProteinCategory.POULTRY);
        addRule("鸽子", ProteinCategory.POULTRY);
        addRule("乳鸽", ProteinCategory.POULTRY);

        // ========== 畜类瘦肉 ==========
        addRule("牛肉", ProteinCategory.LEAN_MEAT);
        addRule("牛排", ProteinCategory.LEAN_MEAT);
        addRule("牛腩", ProteinCategory.LEAN_MEAT);
        addRule("牛里脊", ProteinCategory.LEAN_MEAT);
        addRule("牛腱", ProteinCategory.LEAN_MEAT);
        addRule("牛柳", ProteinCategory.LEAN_MEAT);
        addRule("酱牛肉", ProteinCategory.LEAN_MEAT);
        addRule("卤牛肉", ProteinCategory.LEAN_MEAT);
        addRule("红烧牛肉", ProteinCategory.LEAN_MEAT);
        addRule("瘦猪肉", ProteinCategory.LEAN_MEAT);
        addRule("猪里脊", ProteinCategory.LEAN_MEAT);
        addRule("猪瘦肉", ProteinCategory.LEAN_MEAT);
        addRule("瘦肉", ProteinCategory.LEAN_MEAT);
        addRule("里脊肉", ProteinCategory.LEAN_MEAT);
        addRule("肉丝", ProteinCategory.LEAN_MEAT);
        addRule("肉片", ProteinCategory.LEAN_MEAT);
        addRule("肉末", ProteinCategory.LEAN_MEAT);
        addRule("羊肉", ProteinCategory.LEAN_MEAT);
        addRule("羊排", ProteinCategory.LEAN_MEAT);
        addRule("羊腿", ProteinCategory.LEAN_MEAT);
        addRule("涮羊肉", ProteinCategory.LEAN_MEAT);
        addRule("烤羊肉", ProteinCategory.LEAN_MEAT);
        addRule("羊肉串", ProteinCategory.LEAN_MEAT);
        addRule("兔肉", ProteinCategory.LEAN_MEAT);
        addRule("驴肉", ProteinCategory.LEAN_MEAT);

        // ========== 大豆及制品 ==========
        addRule("豆腐", ProteinCategory.SOY);
        addRule("豆干", ProteinCategory.SOY);
        addRule("豆腐干", ProteinCategory.SOY);
        addRule("豆浆", ProteinCategory.SOY);
        addRule("豆皮", ProteinCategory.SOY);
        addRule("腐竹", ProteinCategory.SOY);
        addRule("豆腐皮", ProteinCategory.SOY);
        addRule("千张", ProteinCategory.SOY);
        addRule("百叶", ProteinCategory.SOY);
        addRule("素鸡", ProteinCategory.SOY);
        addRule("豆腐泡", ProteinCategory.SOY);
        addRule("油豆腐", ProteinCategory.SOY);
        addRule("臭豆腐", ProteinCategory.SOY);
        addRule("豆腐脑", ProteinCategory.SOY);
        addRule("豆花", ProteinCategory.SOY);
        addRule("豆制品", ProteinCategory.SOY);
        addRule("黄豆", ProteinCategory.SOY);
        addRule("大豆", ProteinCategory.SOY);
        addRule("黑豆", ProteinCategory.SOY);
        addRule("青豆", ProteinCategory.SOY);
        addRule("毛豆", ProteinCategory.SOY);
        addRule("纳豆", ProteinCategory.SOY);
        addRule("豆芽", ProteinCategory.SOY);
        addRule("麻婆豆腐", ProteinCategory.SOY);
        addRule("家常豆腐", ProteinCategory.SOY);
        addRule("红烧豆腐", ProteinCategory.SOY);
    }

    private static void addRule(String keyword, ProteinCategory category) {
        PROTEIN_RULES.put(keyword, category);
    }

    /**
     * 从食物列表中识别优质蛋白来源
     * 
     * @param foodNames AI 识别出的食物名称列表
     * @return 识别出的优质蛋白食材列表（去重）
     */
    public List<String> identifyHighQualityProteins(List<String> foodNames) {
        if (foodNames == null || foodNames.isEmpty()) {
            log.debug("食物列表为空，无法识别优质蛋白");
            return Collections.emptyList();
        }

        Set<String> proteinSources = new LinkedHashSet<>();

        for (String foodName : foodNames) {
            if (foodName == null || foodName.isEmpty()) {
                continue;
            }

            // 遍历规则表，查找匹配的优质蛋白关键词
            for (Map.Entry<String, ProteinCategory> entry : PROTEIN_RULES.entrySet()) {
                String keyword = entry.getKey();
                if (foodName.contains(keyword)) {
                    // 添加匹配到的关键词作为优质蛋白来源
                    proteinSources.add(keyword);
                    log.trace("识别到优质蛋白: {} (来自食物: {}, 分类: {})",
                            keyword, foodName, entry.getValue().getDisplayName());
                }
            }
        }

        List<String> result = new ArrayList<>(proteinSources);
        log.info("优质蛋白识别完成，共识别到 {} 种: {}", result.size(), result);
        return result;
    }

    /**
     * 从文本描述中识别优质蛋白来源
     * 支持将整个食物描述作为单个字符串传入
     * 
     * @param foodDescription 食物描述文本
     * @return 识别出的优质蛋白食材列表（去重）
     */
    public List<String> identifyHighQualityProteinsFromText(String foodDescription) {
        if (foodDescription == null || foodDescription.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> proteinSources = new LinkedHashSet<>();

        for (Map.Entry<String, ProteinCategory> entry : PROTEIN_RULES.entrySet()) {
            String keyword = entry.getKey();
            if (foodDescription.contains(keyword)) {
                proteinSources.add(keyword);
            }
        }

        return new ArrayList<>(proteinSources);
    }

    /**
     * 合并多个优质蛋白列表（用于 dashboard 聚合场景）
     * 
     * @param proteinLists 多个优质蛋白列表
     * @return 合并后的去重列表
     */
    public List<String> mergeProteinLists(List<List<String>> proteinLists) {
        if (proteinLists == null || proteinLists.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> merged = new LinkedHashSet<>();
        for (List<String> list : proteinLists) {
            if (list != null) {
                merged.addAll(list);
            }
        }

        return new ArrayList<>(merged);
    }

    /**
     * 生成优质蛋白摘要文本
     * 
     * @param proteins 优质蛋白列表
     * @return 格式化的摘要文本
     */
    public String generateProteinSummary(List<String> proteins) {
        if (proteins == null || proteins.isEmpty()) {
            return "本餐未识别到明显的优质蛋白来源";
        }

        // 按分类分组
        Map<ProteinCategory, List<String>> grouped = new LinkedHashMap<>();
        for (String protein : proteins) {
            ProteinCategory category = PROTEIN_RULES.get(protein);
            if (category != null) {
                grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(protein);
            }
        }

        if (grouped.isEmpty()) {
            return "本餐的优质蛋白来源：" + String.join("、", proteins);
        }

        List<String> parts = grouped.entrySet().stream()
                .map(entry -> String.join("、", entry.getValue()) + "（" + entry.getKey().getDisplayName() + "）")
                .collect(Collectors.toList());

        return "本餐的优质蛋白来源：" + String.join("，", parts);
    }

    /**
     * 获取某个食材的蛋白质分类
     * 
     * @param foodName 食材名称
     * @return 分类枚举，未找到返回 null
     */
    public ProteinCategory getCategory(String foodName) {
        if (foodName == null) {
            return null;
        }
        for (Map.Entry<String, ProteinCategory> entry : PROTEIN_RULES.entrySet()) {
            if (foodName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 获取所有支持的食物关键词
     */
    public Set<String> getAllKeywords() {
        return Collections.unmodifiableSet(PROTEIN_RULES.keySet());
    }
}
