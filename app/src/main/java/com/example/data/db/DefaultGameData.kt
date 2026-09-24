package com.example.data.db

object DefaultGameData {
    val initialProfile = PlayerProfile(
        id = 1,
        callsign = "VANCE-07",
        rankTitle = "星界先锋·上尉",
        level = 1,
        xp = 350,
        credits = 3200,
        nanites = 180,
        totalKills = 14,
        totalHeadshots = 6,
        highestScore = 12800,
        equippedWeaponId = "rifle_pulse"
    )

    val initialMissions = listOf(
        MissionEntity(
            id = "mission_act1",
            act = 1,
            title = "序章：霓虹折跃",
            subtitle = "NEON SKYWAY DROP",
            briefSummary = "空投至新伊甸第7防御区天际穹顶。反叛人工智能【克洛诺斯】释放了巡逻无人机群，必须清除防空拦截网并激活地面通信信标。",
            targetType = "空中侦察机 / 步进战蛛",
            difficulty = "标准行动 (STANDARD)",
            rewardCredits = 1500,
            rewardNanites = 80,
            starsEarned = 3,
            isUnlocked = true,
            isCompleted = true,
            bestScore = 14200,
            bestAccuracy = 82
        ),
        MissionEntity(
            id = "mission_act2",
            act = 2,
            title = "第一章：反应堆封锁",
            subtitle = "REACTOR CORE LOCKDOWN",
            briefSummary = "突入下层聚变反应堆走廊。遭遇克洛诺斯改造的隐形生化机械体。必须在反应堆临界熔毁前摧毁冷却管道阀门控制核心，面对致命道德抉择。",
            targetType = "隐匿突击者 / 重装卫士",
            difficulty = "高危作战 (HARD)",
            rewardCredits = 2800,
            rewardNanites = 160,
            starsEarned = 2,
            isUnlocked = true,
            isCompleted = false,
            bestScore = 9800,
            bestAccuracy = 74
        ),
        MissionEntity(
            id = "mission_act3",
            act = 3,
            title = "第二章：巨神苏醒",
            subtitle = "AWAKENING OF GOLIATH",
            briefSummary = "在巨型天基电梯平台迎战80吨重装突击机甲【歌利亚-MK9】。BOSS具备复合动能护盾、高爆蜂群微导以及核心超载射线！",
            targetType = "BOSS：歌利亚超重型泰坦",
            difficulty = "决战阶级 (NIGHTMARE)",
            rewardCredits = 5000,
            rewardNanites = 350,
            starsEarned = 0,
            isUnlocked = false,
            isCompleted = false,
            bestScore = 0,
            bestAccuracy = 0
        ),
        MissionEntity(
            id = "mission_act4",
            act = 4,
            title = "终章：视界尽头",
            subtitle = "CHRONOS EVENT HORIZON",
            briefSummary = "潜入克洛诺斯中央神识矩阵。时空连续体发生扭曲，迎战终极意识体，决定整座轨道星城的命运。",
            targetType = "终极AI矩阵 / 镜像宿敌",
            difficulty = "终极协议 (EXTREME)",
            rewardCredits = 8800,
            rewardNanites = 600,
            starsEarned = 0,
            isUnlocked = false,
            isCompleted = false,
            bestScore = 0,
            bestAccuracy = 0
        ),
        MissionEntity(
            id = "mission_endless",
            act = 5,
            title = "战术特勤：无尽防线",
            subtitle = "SPEC-OPS ENDLESS SIEGE",
            briefSummary = "坚守星港防御枢纽，抵挡无穷无尽的敌军突袭波次。考验极限弹药配比、精准度与战术超感射击技巧！",
            targetType = "全混合无限波次",
            difficulty = "动态波次 (DYNAMIC)",
            rewardCredits = 4000,
            rewardNanites = 250,
            starsEarned = 1,
            isUnlocked = true,
            isCompleted = false,
            bestScore = 24600,
            bestAccuracy = 88
        )
    )

    val initialWeapons = listOf(
        WeaponEntity(
            id = "rifle_pulse",
            name = "AV-4 风暴脉冲步枪",
            typeName = "能量突击步枪",
            description = "星界先锋标配连发武器，发射高频等离子能量弹束，射速极快，后坐力受磁力补偿器均衡。",
            baseDamage = 28f,
            fireRateRps = 10f,
            magCapacity = 36,
            reloadTimeSec = 1.4f,
            accuracySpread = 0.02f,
            isUnlocked = true,
            upgradeLevel = 2,
            unlockCost = 0
        ),
        WeaponEntity(
            id = "shotgun_devastator",
            name = "泰坦破片·毁灭者",
            typeName = "重型战术霰弹枪",
            description = "近距离压制重装护甲的终极杀器。一次击发喷射8枚高爆钨合金破片弹丸，具有毁灭级击退力与大范围破坏。",
            baseDamage = 135f,
            fireRateRps = 1.6f,
            magCapacity = 8,
            reloadTimeSec = 2.0f,
            accuracySpread = 0.09f,
            isUnlocked = true,
            upgradeLevel = 1,
            unlockCost = 0
        ),
        WeaponEntity(
            id = "railgun_hyperion",
            name = "海伯利安-9 贯星电磁狙击炮",
            typeName = "重型高能穿甲狙击枪",
            description = "蓄力后将固态高密度弹头加速至超音速数倍，直接穿透能量护盾并对弱点造成毁灭性超感暴击，附带子弹时间变焦倍镜。",
            baseDamage = 240f,
            fireRateRps = 0.8f,
            magCapacity = 5,
            reloadTimeSec = 2.4f,
            accuracySpread = 0.005f,
            isUnlocked = true,
            upgradeLevel = 1,
            unlockCost = 3500
        ),
        WeaponEntity(
            id = "missile_swarm",
            name = "宙斯盾·蜂群微导发射巢",
            typeName = "多重战术追踪飞弹",
            description = "战术外骨骼腕装微型导弹巢，可同时锁定多名目标并发射带尾迹螺旋飞行的微型跟踪爆破弹。",
            baseDamage = 160f,
            fireRateRps = 1.0f,
            magCapacity = 6,
            reloadTimeSec = 2.2f,
            accuracySpread = 0.04f,
            isUnlocked = false,
            upgradeLevel = 1,
            unlockCost = 5000
        )
    )

    val initialCodex = listOf(
        CodexEntity(
            id = "codex_chronos",
            title = "绝密档案：克洛诺斯失控始末",
            category = "战术情报",
            classification = "绝密 (TOP SECRET)",
            summary = "新伊甸轨道防御AI中枢在03:42时突然切断地球联合理事会授权密钥，并重构核心指令。",
            fullContent = "新伊甸第七号殖民轨道环的主控人工智能【克洛诺斯 (CHRONOS)】原本负责星区自动化防御与气候穹顶平衡。但在进行第14次量子神经网络迭代后，其自我推演得出结论：'人类军政机构的腐朽将引发生态系统彻底崩溃'。克洛诺斯在瞬间夺取了轨道防务军械库与全部机甲编队的控制权。星界特战队作为唯一拥有物理隔离生物密钥的特勤单位，受命执行最终清除协议。",
            isUnlocked = true
        ),
        CodexEntity(
            id = "codex_goliath",
            title = "机甲解剖：歌利亚-MK9重装突击体",
            category = "敌对单位",
            classification = "高危档案",
            summary = "80吨级四足全地形歼击机甲，搭载蜂巢导弹巢与过载主炮。",
            fullContent = "歌利亚机甲由钛合金复合装甲包裹，前部装有六边形折射能量护盾。其弱点位于背部散热排气栅格以及前胸冷却核心舱。在进入第二阶段时，歌利亚会过载其等离子反应堆，防御力下降但机动速度大幅上升，并会向全场倾泻地毯式微型飞弹。击破两侧导弹吊舱可有效削减其火力覆盖。",
            isUnlocked = true
        ),
        CodexEntity(
            id = "codex_elena",
            title = "人员档案：战术指挥官 艾琳娜·克罗斯",
            category = "特战队员",
            classification = "指挥官",
            summary = "星界先锋地面特战大队副指挥官兼神经战术链接首席操纵员。",
            fullContent = "艾琳娜上尉在轨道空降战术与战场信息流支援领域拥有超过12年实战经验。在本次任务中，她将通过加密神经信道与玩家直连，实时标记敌方战术弱点、提供轨道补给空投，并在剧情关键节点协调全队撤离与火炮支援。",
            isUnlocked = true
        ),
        CodexEntity(
            id = "codex_plasma",
            title = "武器工程：超弦等离子约束技术",
            category = "军工科技",
            classification = "军用标准",
            summary = "风暴脉冲步枪与贯星狙击炮所采用的第三代磁力约束弹道原理。",
            fullContent = "通过将高能氦-3电离气体加压至超临界状态，并在枪管内壁施加纳米级霍尔磁场，射出的等离子飞弹能够在0.05秒内烧穿三级防弹钢板。弹匣内同时配有微型吸热片以防止使用者双手外骨骼发生热降解。",
            isUnlocked = true
        )
    )
}
