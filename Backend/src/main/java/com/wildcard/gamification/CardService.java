package com.wildcard.gamification;

import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CardService {

    private final XpLogRepository xpLogRepository;
    private final UserRepository userRepository;

    public int levelFor(long totalXp) {
        return LevelCurve.levelFor(totalXp);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recompute(User user) {
        Map<XpCategory, Integer> byCategory = new EnumMap<>(XpCategory.class);
        for (XpCategory category : XpCategory.values()) {
            byCategory.put(category, 0);
        }

        List<Object[]> rows = xpLogRepository.sumByCategory(user.getId());
        for (Object[] row : rows) {
            byCategory.put((XpCategory) row[0], ((Number) row[1]).intValue());
        }

        int ani = byCategory.get(XpCategory.ANI);
        int gam = byCategory.get(XpCategory.GAM);
        int mus = byCategory.get(XpCategory.MUS);
        int cha = byCategory.get(XpCategory.CHA);
        int totalXp = ani + gam + mus + cha;

        int level = levelFor(totalXp);

        user.setAni(ani);
        user.setGam(gam);
        user.setMus(mus);
        user.setCha(cha);
        user.setTotalXp(totalXp);
        user.setLevel(level);
        user.setRarity(Rarity.fromLevel(level).name());
        user.setTitle(TitleRule.pick(ani, gam, mus, cha));

        userRepository.save(user);
    }
}