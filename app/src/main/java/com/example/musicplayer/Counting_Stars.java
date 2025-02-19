package com.example.musicplayer;

public class Counting_Stars {
    // 歌词时间戳数组（单位：毫秒）
    public static final long[][] LYRIC_TIMES = {
            { 790 }, // [00:00.79]
            { 5490 }, // [00:05.49]
            { 9390 }, // [00:09.39]
            { 14360 }, // [00:14.36]
            { 16360 }, // [00:16.36]
            { 19280 }, // [00:19.28]
            { 38050 }, // [00:38.05]
            { 40670 }, // [00:40.67]
            { 42580 }, // [00:42.58]
            { 44570 }, // [00:44.57]
            { 46470 }, // [00:46.47]
            { 48510 }, // [00:48.51]
            { 50430 }, // [00:50.43]
            { 52540 }, // [00:52.54]
            { 55030 }, // [00:55.03]
            { 59230 }, // [00:59.23]
            { 62680 }, // [01:02.68]
            { 67030 }, // [01:07.03]
            { 70450 }, // [01:10.45]
            { 74200 }, // [01:14.20]
            { 78160 }, // [01:18.16]
            { 81930 }, // [01:21.93]
            { 85750 }, // [01:25.75]
            { 89760 }, // [01:29.76]
            { 91710 }, // [01:31.71]
            { 93840 }, // [01:33.84]
            { 97720 }, // [01:37.72]
            { 101640 }, // [01:41.64]
            { 105560 }, // [01:45.56]
            { 107520 }, // [01:47.52]
            { 116250 }, // [01:56.25]
            { 119450 }, // [01:59.45]
            { 121330 }, // [02:01.33]
            { 123220 }, // [02:03.22]
            { 125140 }, // [02:05.14]
            { 127340 }, // [02:07.34]
            { 129130 }, // [02:09.13]
            { 131280 }, // [02:11.28]
            { 133440 }, // [02:13.44]
            { 137840 }, // [02:17.84]
            { 141280 }, // [02:21.28]
            { 145070 }, // [02:25.07]
            { 148900 }, // [02:28.90]
            { 152810 }, // [02:32.81]
            { 156600 }, // [02:36.60]
            { 160590 }, // [02:40.59]
            { 162610 }, // [02:42.61]
            { 164720 }, // [02:44.72]
            { 168710 }, // [02:48.71]
            { 172400 }, // [02:52.40]
            { 176330 }, // [02:56.33]
            { 178150 }, // [02:58.15]
            { 184420 }, // [03:04.42]
            { 185020 }, // [03:05.02]
            { 185850 }, // [03:05.85]
            { 186880 }, // [03:06.88]
            { 188050 }, // [03:08.05]
            { 188730 }, // [03:08.73]
            { 189920 }, // [03:09.92]
            { 190890 }, // [03:10.89]
            { 192010 }, // [03:12.01]
            { 192830 }, // [03:12.83]
            { 193860 }, // [03:13.86]
            { 194850 }, // [03:14.85]
            { 196020 }, // [03:16.02]
            { 196910 }, // [03:16.91]
            { 197840 }, // [03:17.84]
            { 198830 }, // [03:18.83]
            { 199980 }, // [03:19.98]
            { 205860 }, // [03:25.86]
            { 207060 }, // [03:27.06]
            { 210760 }, // [03:30.76]
            { 214620 }, // [03:34.62]
            { 218630 }, // [03:38.63]
            { 220540 }, // [03:40.54]
            { 222480 }, // [03:42.48]
            { 226660 }, // [03:46.66]
            { 230510 }, // [03:50.51]
            { 234520 }, // [03:54.52]
            { 236460 }, // [03:56.46]
            { 239070 }, // [03:59.07]
            { 239620 }, // [03:59.62]
            { 240110 }, // [04:00.11]
            { 240810 }, // [04:00.81]
            { 242230 }, // [04:02.23]
            { 243030 }, // [04:03.03]
            { 243970 }, // [04:03.97]
            { 245000 }, // [04:05.00]
            { 246180 }, // [04:06.18]
            { 247030 }, // [04:07.03]
            { 247680 }, // [04:07.68]
            { 248990 }, // [04:08.99]
            { 249830 }, // [04:09.83]
            { 250880 }, // [04:10.88]
            { 251660 }, // [04:11.66]
            { 252600 }, // [04:12.60]
            { 257060 } // [04:17.06]
    };

    public static final String[] LYRIC_TEXTS = {
            "Lately I've been I've been losing sleep\n最近我总是辗转反侧 难以入眠",
            "Dreaming about the things that we could be\n对我们曾有过的愿景浮想联翩",
            "But baby I've been I've been praying hard\n但亲爱的 我早已在内心深处祈祷着",
            "Said no more counting dollars\n祈祷自己不再迷失于金钱的追逐中",
            "We'll be counting stars\n我们可以细数满天繁星",
            "Yeah we'll be counting stars\n是啊 我们可以细数满天繁星",
            "I see this life like a swinging vine\n生活就像一株跃动的藤蔓",
            "Swing my heart across the line\n长驱直入 激活我的内心",
            "In my face is flashing signs\n我能感受到太阳的耀眼",
            "Seek it out and ye' shall find\n遍寻之后你就会发现",
            "Old but I'm not that old\n我虽上了年纪 但也不是老态龙钟",
            "Young but I'm not that bold\n虽还年轻 却未必鲁莽失礼",
            "And I don't think the world is sold\n坚信这个世界美好如初",
            "On just doing what we're told\n我只是循规蹈矩地生活着",
            "I I I I feel something so right\n惯于离经叛道中",
            "Doing the wrong thing\n体味心安理得",
            "I I I I feel something so wrong\n亦于按部就班中",
            "Doing the right thing\n痛感乏善可陈",
            "I couldn't lie couldn't lie couldn't lie\n我不可以自欺欺人 不可以自欺欺人",
            "Everything that kills me makes me feel alive\n但置我于死地者 必将赐我以后生",
            "Lately I've been I've been losing sleep\n最近我总是辗转反侧 难以入眠",
            "Dreaming about the things that we could be\n对我们曾有过的愿景浮想联翩",
            "But baby I've been I've been praying hard\n但亲爱的 我早已在内心深处祈祷着",
            "Said no more counting dollars\n祈祷自己不再迷失于金钱的追逐中",
            "We'll be counting stars\n我们可以细数满天繁星",
            "Lately I've been I've been losing sleep\n最近我总是辗转反侧 难以入眠",
            "Dreaming about the things that we could be\n对我们曾有过的愿景浮想联翩",
            "But baby I've been I've been praying hard\n但亲爱的 我早已在内心深处祈祷着",
            "Said no more counting dollars\n祈祷自己不再迷失于金钱的追逐中",
            "We'll be we'll be counting stars\n我们可以细数满天繁星",
            "I feel your love and I feel it burn\n我感觉到爱了 我感觉到它正在燃烧",
            "Down this river every turn\n于河流的每个迂回处翻腾",
            "Hope is our four-letter word\n希望只是个四字单词",
            "Make that money watch it burn\n身外之物 皆可抛却",
            "Old but I'm not that old\n我虽上了年纪 但也不是老态龙钟",
            "Young but I'm not that bold\n虽还年轻 却未必鲁莽失礼",
            "And I don't think the world is sold\n坚信这个世界美好如初",
            "On just doing what we're told\n我只是循规蹈矩地生活着",
            "I I I I feel something so wrong\n亦于按部就班中",
            "Doing the right thing\n痛感乏善可陈",
            "I couldn't lie couldn't lie couldn't lie\n我不可以自欺欺人 不可以自欺欺人",
            "Everything that drowns me makes me wanna fly\n一切淹没我的东西 总是想让我飞翔",
            "Lately I've been I've been losing sleep\n最近我总是辗转反侧 难以入眠",
            "Dreaming about the things that we could be\n对我们曾有过的愿景浮想联翩",
            "But baby I've been I've been praying hard\n但亲爱的 我早已在内心深处祈祷着",
            "Said no more counting dollars\n祈祷自己不再迷失于金钱的追逐中",
            "We'll be counting stars\n我们可以细数满天繁星",
            "Lately I've been I've been losing sleep\n最近我总是辗转反侧 难以入眠",
            "Dreaming about the things that we could be\n对我们曾有过的愿景浮想联翩",
            "But baby I've been I've been praying hard\n但亲爱的 我早已在内心深处祈祷着",
            "Said no more counting dollars\n祈祷自己不再迷失于金钱的追逐中",
            "We'll be we'll be counting stars\n我们可以细数满天繁星",
            "Take that money\n身外之物",
            "Watch it burn\n皆可抛却",
            "Sink in the river\n泛舟当歌",
            "The lessons I've learned\n人生几何",
            "Take that money\n身外之物",
            "Watch it burn\n皆可抛却",
            "Sink in the river\n泛舟当歌",
            "The lessons I've learned\n人生几何",
            "Take that money\n身外之物",
            "Watch it burn\n皆可抛却",
            "Sink in the river\n泛舟当歌",
            "The lessons I've learned\n人生几何",
            "Take that money\n身外之物",
            "Watch it burn\n皆可抛却",
            "Sink in the river\n泛舟当歌",
            "The lessons I've learned\n人生几何",
            "Everything that kills me\n但置我于死地者",
            "Makes me feel alive\n必将赐我以后生",
            "Lately I've been I've been losing sleep\n最近我总是辗转反侧 难以入眠",
            "Dreaming about the things that we could be\n对我们曾有过的愿景浮想联翩",
            "But baby I've been I've been praying hard\n但亲爱的 我早已在内心深处祈祷着",
            "Said no more counting dollars\n祈祷自己不再迷失于金钱的追逐中",
            "We'll be counting stars\n我们可以细数满天繁星",
            "Lately I've been I've been losing sleep\n最近我总是辗转反侧 难以入眠",
            "Dreaming about the things that we could be\n对我们曾有过的愿景浮想联翩",
            "But baby I've been I've been praying hard\n但亲爱的 我早已在内心深处祈祷着",
            "Said no more counting dollars\n祈祷自己不再迷失于金钱的追逐中",
            "We'll be we'll be counting stars\n我们可以细数满天繁星",
            "Take that money\n身外之物",
            "Watch it burn\n皆可抛却",
            "Sink in the river\n泛舟当歌",
            "The lessons I've learned\n人生几何",
            "Take that money\n身外之物",
            "Watch it burn\n皆可抛却",
            "Sink in the river\n泛舟当歌",
            "The lessons I've learned\n人生几何",
            "Take that money\n身外之物",
            "Watch it burn\n皆可抛却",
            "Sink in the river\n泛舟当歌",
            "The lessons I've learned\n人生几何",
            "Take that money\n身外之物",
            "Watch it burn\n皆可抛却",
            "Sink in the river\n泛舟当歌",
            "The lessons I've learned\n人生几何"
    };

    public static final String SINGER = "OneRepublic";

    // 包含空数组保护的歌词查找方法
    public static int findLyricIndex(long position) {
        // 空数组保护（重要！）
        if (LYRIC_TIMES.length == 0) {
            return 0;
        }

        // 逆序查找逻辑
        for (int i = LYRIC_TIMES.length - 1; i >= 0; i--) {
            if (position >= LYRIC_TIMES[i][0]) {
                return i;
            }
        }
        return 0;
    }
}
