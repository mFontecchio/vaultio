package com.mrhayami.vaultio.data

object PokemonUtils {

    /**
     * Static National Pokédex number lookup.
     * Keys are lowercase Pokémon base names (including common aliases).
     * Covers all 1025 species through Scarlet/Violet + DLC (Gen 1–9).
     */
    val POKEMON_DEX_MAP: Map<String, Int> = mapOf(
        // ── Gen 1 ──────────────────────────────────────────
        "bulbasaur" to 1, "ivysaur" to 2, "venusaur" to 3,
        "charmander" to 4, "charmeleon" to 5, "charizard" to 6,
        "squirtle" to 7, "wartortle" to 8, "blastoise" to 9,
        "caterpie" to 10, "metapod" to 11, "butterfree" to 12,
        "weedle" to 13, "kakuna" to 14, "beedrill" to 15,
        "pidgey" to 16, "pidgeotto" to 17, "pidgeot" to 18,
        "rattata" to 19, "raticate" to 20,
        "spearow" to 21, "fearow" to 22,
        "ekans" to 23, "arbok" to 24,
        "pikachu" to 25, "raichu" to 26,
        "sandshrew" to 27, "sandslash" to 28,
        "nidoran♀" to 29, "nidoran-f" to 29, "nidoran female" to 29,
        "nidorina" to 30, "nidoqueen" to 31,
        "nidoran♂" to 32, "nidoran-m" to 32, "nidoran male" to 32,
        "nidorino" to 33, "nidoking" to 34,
        "clefairy" to 35, "clefable" to 36,
        "vulpix" to 37, "ninetales" to 38,
        "jigglypuff" to 39, "wigglytuff" to 40,
        "zubat" to 41, "golbat" to 42,
        "oddish" to 43, "gloom" to 44, "vileplume" to 45,
        "paras" to 46, "parasect" to 47,
        "venonat" to 48, "venomoth" to 49,
        "diglett" to 50, "dugtrio" to 51,
        "meowth" to 52, "persian" to 53,
        "psyduck" to 54, "golduck" to 55,
        "mankey" to 56, "primeape" to 57,
        "growlithe" to 58, "arcanine" to 59,
        "poliwag" to 60, "poliwhirl" to 61, "poliwrath" to 62,
        "abra" to 63, "kadabra" to 64, "alakazam" to 65,
        "machop" to 66, "machoke" to 67, "machamp" to 68,
        "bellsprout" to 69, "weepinbell" to 70, "victreebel" to 71,
        "tentacool" to 72, "tentacruel" to 73,
        "geodude" to 74, "graveler" to 75, "golem" to 76,
        "ponyta" to 77, "rapidash" to 78,
        "slowpoke" to 79, "slowbro" to 80,
        "magnemite" to 81, "magneton" to 82,
        "farfetch'd" to 83, "farfetchd" to 83, "farfetch" to 83,
        "doduo" to 84, "dodrio" to 85,
        "seel" to 86, "dewgong" to 87,
        "grimer" to 88, "muk" to 89,
        "shellder" to 90, "cloyster" to 91,
        "gastly" to 92, "haunter" to 93, "gengar" to 94,
        "onix" to 95,
        "drowzee" to 96, "hypno" to 97,
        "krabby" to 98, "kingler" to 99,
        "voltorb" to 100, "electrode" to 101,
        "exeggcute" to 102, "exeggutor" to 103,
        "cubone" to 104, "marowak" to 105,
        "hitmonlee" to 106, "hitmonchan" to 107,
        "lickitung" to 108,
        "koffing" to 109, "weezing" to 110,
        "rhyhorn" to 111, "rhydon" to 112,
        "chansey" to 113, "tangela" to 114, "kangaskhan" to 115,
        "horsea" to 116, "seadra" to 117,
        "goldeen" to 118, "seaking" to 119,
        "staryu" to 120, "starmie" to 121,
        "mr. mime" to 122, "mr mime" to 122, "mr.mime" to 122,
        "scyther" to 123, "jynx" to 124,
        "electabuzz" to 125, "magmar" to 126,
        "pinsir" to 127, "tauros" to 128,
        "magikarp" to 129, "gyarados" to 130,
        "lapras" to 131, "ditto" to 132,
        "eevee" to 133, "vaporeon" to 134, "jolteon" to 135, "flareon" to 136,
        "porygon" to 137,
        "omanyte" to 138, "omastar" to 139,
        "kabuto" to 140, "kabutops" to 141,
        "aerodactyl" to 142, "snorlax" to 143,
        "articuno" to 144, "zapdos" to 145, "moltres" to 146,
        "dratini" to 147, "dragonair" to 148, "dragonite" to 149,
        "mewtwo" to 150, "mew" to 151,

        // ── Gen 2 ──────────────────────────────────────────
        "chikorita" to 152, "bayleef" to 153, "meganium" to 154,
        "cyndaquil" to 155, "quilava" to 156, "typhlosion" to 157,
        "totodile" to 158, "croconaw" to 159, "feraligatr" to 160,
        "sentret" to 161, "furret" to 162,
        "hoothoot" to 163, "noctowl" to 164,
        "ledyba" to 165, "ledian" to 166,
        "spinarak" to 167, "ariados" to 168,
        "crobat" to 169,
        "chinchou" to 170, "lanturn" to 171,
        "pichu" to 172, "cleffa" to 173, "igglybuff" to 174,
        "togepi" to 175, "togetic" to 176,
        "natu" to 177, "xatu" to 178,
        "mareep" to 179, "flaaffy" to 180, "ampharos" to 181,
        "bellossom" to 182,
        "marill" to 183, "azumarill" to 184,
        "sudowoodo" to 185, "politoed" to 186,
        "hoppip" to 187, "skiploom" to 188, "jumpluff" to 189,
        "aipom" to 190,
        "sunkern" to 191, "sunflora" to 192,
        "yanma" to 193,
        "wooper" to 194, "quagsire" to 195,
        "espeon" to 196, "umbreon" to 197,
        "murkrow" to 198, "slowking" to 199, "misdreavus" to 200,
        "unown" to 201, "wobbuffet" to 202,
        "girafarig" to 203,
        "pineco" to 204, "forretress" to 205,
        "dunsparce" to 206, "gligar" to 207,
        "steelix" to 208,
        "snubbull" to 209, "granbull" to 210,
        "qwilfish" to 211, "scizor" to 212, "shuckle" to 213,
        "heracross" to 214, "sneasel" to 215,
        "teddiursa" to 216, "ursaring" to 217,
        "slugma" to 218, "magcargo" to 219,
        "swinub" to 220, "piloswine" to 221,
        "corsola" to 222,
        "remoraid" to 223, "octillery" to 224,
        "delibird" to 225, "mantine" to 226, "skarmory" to 227,
        "houndour" to 228, "houndoom" to 229,
        "kingdra" to 230,
        "phanpy" to 231, "donphan" to 232,
        "porygon2" to 233,
        "stantler" to 234, "smeargle" to 235,
        "tyrogue" to 236, "hitmontop" to 237,
        "smoochum" to 238, "elekid" to 239, "magby" to 240,
        "miltank" to 241, "blissey" to 242,
        "raikou" to 243, "entei" to 244, "suicune" to 245,
        "larvitar" to 246, "pupitar" to 247, "tyranitar" to 248,
        "lugia" to 249, "ho-oh" to 250, "ho oh" to 250,
        "celebi" to 251,

        // ── Gen 3 ──────────────────────────────────────────
        "treecko" to 252, "grovyle" to 253, "sceptile" to 254,
        "torchic" to 255, "combusken" to 256, "blaziken" to 257,
        "mudkip" to 258, "marshtomp" to 259, "swampert" to 260,
        "poochyena" to 261, "mightyena" to 262,
        "zigzagoon" to 263, "linoone" to 264,
        "wurmple" to 265, "silcoon" to 266, "beautifly" to 267,
        "cascoon" to 268, "dustox" to 269,
        "lotad" to 270, "lombre" to 271, "ludicolo" to 272,
        "seedot" to 273, "nuzleaf" to 274, "shiftry" to 275,
        "taillow" to 276, "swellow" to 277,
        "wingull" to 278, "pelipper" to 279,
        "ralts" to 280, "kirlia" to 281, "gardevoir" to 282,
        "surskit" to 283, "masquerain" to 284,
        "shroomish" to 285, "breloom" to 286,
        "slakoth" to 287, "vigoroth" to 288, "slaking" to 289,
        "nincada" to 290, "ninjask" to 291, "shedinja" to 292,
        "whismur" to 293, "loudred" to 294, "exploud" to 295,
        "makuhita" to 296, "hariyama" to 297,
        "azurill" to 298, "nosepass" to 299,
        "skitty" to 300, "delcatty" to 301,
        "sableye" to 302, "mawile" to 303,
        "aron" to 304, "lairon" to 305, "aggron" to 306,
        "meditite" to 307, "medicham" to 308,
        "electrike" to 309, "manectric" to 310,
        "plusle" to 311, "minun" to 312,
        "volbeat" to 313, "illumise" to 314,
        "roselia" to 315,
        "gulpin" to 316, "swalot" to 317,
        "carvanha" to 318, "sharpedo" to 319,
        "wailmer" to 320, "wailord" to 321,
        "numel" to 322, "camerupt" to 323,
        "torkoal" to 324,
        "spoink" to 325, "grumpig" to 326,
        "spinda" to 327,
        "trapinch" to 328, "vibrava" to 329, "flygon" to 330,
        "cacnea" to 331, "cacturne" to 332,
        "swablu" to 333, "altaria" to 334,
        "zangoose" to 335, "seviper" to 336,
        "lunatone" to 337, "solrock" to 338,
        "barboach" to 339, "whiscash" to 340,
        "corphish" to 341, "crawdaunt" to 342,
        "baltoy" to 343, "claydol" to 344,
        "lileep" to 345, "cradily" to 346,
        "anorith" to 347, "armaldo" to 348,
        "feebas" to 349, "milotic" to 350,
        "castform" to 351, "kecleon" to 352,
        "shuppet" to 353, "banette" to 354,
        "duskull" to 355, "dusclops" to 356,
        "tropius" to 357, "chimecho" to 358, "absol" to 359,
        "wynaut" to 360,
        "snorunt" to 361, "glalie" to 362,
        "spheal" to 363, "sealeo" to 364, "walrein" to 365,
        "clamperl" to 366, "huntail" to 367, "gorebyss" to 368,
        "relicanth" to 369, "luvdisc" to 370,
        "bagon" to 371, "shelgon" to 372, "salamence" to 373,
        "beldum" to 374, "metang" to 375, "metagross" to 376,
        "regirock" to 377, "regice" to 378, "registeel" to 379,
        "latias" to 380, "latios" to 381,
        "kyogre" to 382, "groudon" to 383, "rayquaza" to 384,
        "jirachi" to 385, "deoxys" to 386,

        // ── Gen 4 ──────────────────────────────────────────
        "turtwig" to 387, "grotle" to 388, "torterra" to 389,
        "chimchar" to 390, "monferno" to 391, "infernape" to 392,
        "piplup" to 393, "prinplup" to 394, "empoleon" to 395,
        "starly" to 396, "staravia" to 397, "staraptor" to 398,
        "bidoof" to 399, "bibarel" to 400,
        "kricketot" to 401, "kricketune" to 402,
        "shinx" to 403, "luxio" to 404, "luxray" to 405,
        "budew" to 406, "roserade" to 407,
        "cranidos" to 408, "rampardos" to 409,
        "shieldon" to 410, "bastiodon" to 411,
        "burmy" to 412, "wormadam" to 413, "mothim" to 414,
        "combee" to 415, "vespiquen" to 416,
        "pachirisu" to 417,
        "buizel" to 418, "floatzel" to 419,
        "cherubi" to 420, "cherrim" to 421,
        "shellos" to 422, "gastrodon" to 423,
        "ambipom" to 424,
        "drifloon" to 425, "drifblim" to 426,
        "buneary" to 427, "lopunny" to 428,
        "mismagius" to 429, "honchkrow" to 430,
        "glameow" to 431, "purugly" to 432,
        "chingling" to 433,
        "stunky" to 434, "skuntank" to 435,
        "bronzor" to 436, "bronzong" to 437,
        "bonsly" to 438,
        "mime jr." to 439, "mime jr" to 439,
        "happiny" to 440, "chatot" to 441, "spiritomb" to 442,
        "gible" to 443, "gabite" to 444, "garchomp" to 445,
        "munchlax" to 446,
        "riolu" to 447, "lucario" to 448,
        "hippopotas" to 449, "hippowdon" to 450,
        "skorupi" to 451, "drapion" to 452,
        "croagunk" to 453, "toxicroak" to 454,
        "carnivine" to 455,
        "finneon" to 456, "lumineon" to 457,
        "mantyke" to 458,
        "snover" to 459, "abomasnow" to 460,
        "weavile" to 461, "magnezone" to 462, "lickilicky" to 463,
        "rhyperior" to 464, "tangrowth" to 465,
        "electivire" to 466, "magmortar" to 467,
        "togekiss" to 468, "yanmega" to 469,
        "leafeon" to 470, "glaceon" to 471,
        "gliscor" to 472, "mamoswine" to 473,
        "porygon-z" to 474,
        "gallade" to 475, "probopass" to 476, "dusknoir" to 477,
        "froslass" to 478, "rotom" to 479,
        "uxie" to 480, "mesprit" to 481, "azelf" to 482,
        "dialga" to 483, "palkia" to 484,
        "heatran" to 485, "regigigas" to 486,
        "giratina" to 487, "cresselia" to 488,
        "phione" to 489, "manaphy" to 490,
        "darkrai" to 491, "shaymin" to 492, "arceus" to 493,

        // ── Gen 5 ──────────────────────────────────────────
        "victini" to 494,
        "snivy" to 495, "servine" to 496, "serperior" to 497,
        "tepig" to 498, "pignite" to 499, "emboar" to 500,
        "oshawott" to 501, "dewott" to 502, "samurott" to 503,
        "patrat" to 504, "watchog" to 505,
        "lillipup" to 506, "herdier" to 507, "stoutland" to 508,
        "purrloin" to 509, "liepard" to 510,
        "pansage" to 511, "simisage" to 512,
        "pansear" to 513, "simisear" to 514,
        "panpour" to 515, "simipour" to 516,
        "munna" to 517, "musharna" to 518,
        "pidove" to 519, "tranquill" to 520, "unfezant" to 521,
        "blitzle" to 522, "zebstrika" to 523,
        "roggenrola" to 524, "boldore" to 525, "gigalith" to 526,
        "woobat" to 527, "swoobat" to 528,
        "drilbur" to 529, "excadrill" to 530,
        "audino" to 531,
        "timburr" to 532, "gurdurr" to 533, "conkeldurr" to 534,
        "tympole" to 535, "palpitoad" to 536, "seismitoad" to 537,
        "throh" to 538, "sawk" to 539,
        "sewaddle" to 540, "swadloon" to 541, "leavanny" to 542,
        "venipede" to 543, "whirlipede" to 544, "scolipede" to 545,
        "cottonee" to 546, "whimsicott" to 547,
        "petilil" to 548, "lilligant" to 549,
        "basculin" to 550,
        "sandile" to 551, "krokorok" to 552, "krookodile" to 553,
        "darumaka" to 554, "darmanitan" to 555,
        "maractus" to 556,
        "dwebble" to 557, "crustle" to 558,
        "scraggy" to 559, "scrafty" to 560,
        "sigilyph" to 561,
        "yamask" to 562, "cofagrigus" to 563,
        "tirtouga" to 564, "carracosta" to 565,
        "archen" to 566, "archeops" to 567,
        "trubbish" to 568, "garbodor" to 569,
        "zorua" to 570, "zoroark" to 571,
        "minccino" to 572, "cinccino" to 573,
        "gothita" to 574, "gothorita" to 575, "gothitelle" to 576,
        "solosis" to 577, "duosion" to 578, "reuniclus" to 579,
        "ducklett" to 580, "swanna" to 581,
        "vanillite" to 582, "vanillish" to 583, "vanilluxe" to 584,
        "deerling" to 585, "sawsbuck" to 586,
        "emolga" to 587,
        "karrablast" to 588, "escavalier" to 589,
        "foongus" to 590, "amoonguss" to 591,
        "frillish" to 592, "jellicent" to 593,
        "alomomola" to 594,
        "joltik" to 595, "galvantula" to 596,
        "ferroseed" to 597, "ferrothorn" to 598,
        "klink" to 599, "klang" to 600, "klinklang" to 601,
        "tynamo" to 602, "eelektrik" to 603, "eelektross" to 604,
        "elgyem" to 605, "beheeyem" to 606,
        "litwick" to 607, "lampent" to 608, "chandelure" to 609,
        "axew" to 610, "fraxure" to 611, "haxorus" to 612,
        "cubchoo" to 613, "beartic" to 614,
        "cryogonal" to 615,
        "shelmet" to 616, "accelgor" to 617,
        "stunfisk" to 618,
        "mienfoo" to 619, "mienshao" to 620,
        "druddigon" to 621,
        "golett" to 622, "golurk" to 623,
        "pawniard" to 624, "bisharp" to 625,
        "bouffalant" to 626,
        "rufflet" to 627, "braviary" to 628,
        "vullaby" to 629, "mandibuzz" to 630,
        "heatmor" to 631, "durant" to 632,
        "deino" to 633, "zweilous" to 634, "hydreigon" to 635,
        "larvesta" to 636, "volcarona" to 637,
        "cobalion" to 638, "terrakion" to 639, "virizion" to 640,
        "tornadus" to 641, "thundurus" to 642,
        "reshiram" to 643, "zekrom" to 644, "landorus" to 645,
        "kyurem" to 646,
        "keldeo" to 647, "meloetta" to 648, "genesect" to 649,

        // ── Gen 6 ──────────────────────────────────────────
        "chespin" to 650, "quilladin" to 651, "chesnaught" to 652,
        "fennekin" to 653, "braixen" to 654, "delphox" to 655,
        "froakie" to 656, "frogadier" to 657, "greninja" to 658,
        "bunnelby" to 659, "diggersby" to 660,
        "fletchling" to 661, "fletchinder" to 662, "talonflame" to 663,
        "scatterbug" to 664, "spewpa" to 665, "vivillon" to 666,
        "litleo" to 667, "pyroar" to 668,
        "flabébé" to 669, "flabebe" to 669,
        "floette" to 670, "florges" to 671,
        "skiddo" to 672, "gogoat" to 673,
        "pancham" to 674, "pangoro" to 675,
        "furfrou" to 676,
        "espurr" to 677, "meowstic" to 678,
        "honedge" to 679, "doublade" to 680, "aegislash" to 681,
        "spritzee" to 682, "aromatisse" to 683,
        "swirlix" to 684, "slurpuff" to 685,
        "inkay" to 686, "malamar" to 687,
        "binacle" to 688, "barbaracle" to 689,
        "skrelp" to 690, "dragalge" to 691,
        "clauncher" to 692, "clawitzer" to 693,
        "helioptile" to 694, "heliolisk" to 695,
        "tyrunt" to 696, "tyrantrum" to 697,
        "amaura" to 698, "aurorus" to 699,
        "sylveon" to 700,
        "hawlucha" to 701, "dedenne" to 702, "carbink" to 703,
        "goomy" to 704, "sliggoo" to 705, "goodra" to 706,
        "klefki" to 707,
        "phantump" to 708, "trevenant" to 709,
        "pumpkaboo" to 710, "gourgeist" to 711,
        "bergmite" to 712, "avalugg" to 713,
        "noibat" to 714, "noivern" to 715,
        "xerneas" to 716, "yveltal" to 717, "zygarde" to 718,
        "diancie" to 719, "hoopa" to 720, "volcanion" to 721,

        // ── Gen 7 ──────────────────────────────────────────
        "rowlet" to 722, "dartrix" to 723, "decidueye" to 724,
        "litten" to 725, "torracat" to 726, "incineroar" to 727,
        "popplio" to 728, "brionne" to 729, "primarina" to 730,
        "pikipek" to 731, "trumbeak" to 732, "toucannon" to 733,
        "yungoos" to 734, "gumshoos" to 735,
        "grubbin" to 736, "charjabug" to 737, "vikavolt" to 738,
        "crabrawler" to 739, "crabominable" to 740,
        "oricorio" to 741,
        "cutiefly" to 742, "ribombee" to 743,
        "rockruff" to 744, "lycanroc" to 745,
        "wishiwashi" to 746,
        "mareanie" to 747, "toxapex" to 748,
        "mudbray" to 749, "mudsdale" to 750,
        "dewpider" to 751, "araquanid" to 752,
        "fomantis" to 753, "lurantis" to 754,
        "morelull" to 755, "shiinotic" to 756,
        "salandit" to 757, "salazzle" to 758,
        "stufful" to 759, "bewear" to 760,
        "bounsweet" to 761, "steenee" to 762, "tsareena" to 763,
        "comfey" to 764,
        "oranguru" to 765, "passimian" to 766,
        "wimpod" to 767, "golisopod" to 768,
        "sandygast" to 769, "palossand" to 770,
        "pyukumuku" to 771,
        "type: null" to 772, "type null" to 772, "typenull" to 772,
        "silvally" to 773,
        "minior" to 774, "komala" to 775, "turtonator" to 776,
        "togedemaru" to 777, "mimikyu" to 778,
        "bruxish" to 779, "drampa" to 780, "dhelmise" to 781,
        "jangmo-o" to 782, "jangmo o" to 782,
        "hakamo-o" to 783, "hakamo o" to 783,
        "kommo-o" to 784, "kommo o" to 784,
        "tapu koko" to 785, "tapu lele" to 786,
        "tapu bulu" to 787, "tapu fini" to 788,
        "cosmog" to 789, "cosmoem" to 790,
        "solgaleo" to 791, "lunala" to 792,
        "nihilego" to 793, "buzzwole" to 794, "pheromosa" to 795,
        "xurkitree" to 796, "celesteela" to 797, "kartana" to 798,
        "guzzlord" to 799, "necrozma" to 800,
        "magearna" to 801, "marshadow" to 802,
        "poipole" to 803, "naganadel" to 804,
        "stakataka" to 805, "blacephalon" to 806,
        "zeraora" to 807, "meltan" to 808, "melmetal" to 809,

        // ── Gen 8 ──────────────────────────────────────────
        "grookey" to 810, "thwackey" to 811, "rillaboom" to 812,
        "scorbunny" to 813, "raboot" to 814, "cinderace" to 815,
        "sobble" to 816, "drizzile" to 817, "inteleon" to 818,
        "skwovet" to 819, "greedent" to 820,
        "rookidee" to 821, "corvisquire" to 822, "corviknight" to 823,
        "blipbug" to 824, "dottler" to 825, "orbeetle" to 826,
        "nickit" to 827, "thievul" to 828,
        "gossifleur" to 829, "eldegoss" to 830,
        "wooloo" to 831, "dubwool" to 832,
        "chewtle" to 833, "drednaw" to 834,
        "yamper" to 835, "boltund" to 836,
        "rolycoly" to 837, "carkol" to 838, "coalossal" to 839,
        "applin" to 840, "flapple" to 841, "appletun" to 842,
        "silicobra" to 843, "sandaconda" to 844,
        "cramorant" to 845,
        "arrokuda" to 846, "barraskewda" to 847,
        "toxel" to 848, "toxtricity" to 849,
        "sizzlipede" to 850, "centiskorch" to 851,
        "clobbopus" to 852, "grapploct" to 853,
        "sinistea" to 854, "polteageist" to 855,
        "hatenna" to 856, "hattrem" to 857, "hatterene" to 858,
        "impidimp" to 859, "morgrem" to 860, "grimmsnarl" to 861,
        "obstagoon" to 862, "perrserker" to 863, "cursola" to 864,
        "sirfetch'd" to 865, "sirfetchd" to 865,
        "mr. rime" to 866, "mr rime" to 866,
        "runerigus" to 867,
        "milcery" to 868, "alcremie" to 869,
        "falinks" to 870, "pincurchin" to 871,
        "snom" to 872, "frosmoth" to 873,
        "stonjourner" to 874, "eiscue" to 875, "indeedee" to 876,
        "morpeko" to 877,
        "cufant" to 878, "copperajah" to 879,
        "dracozolt" to 880, "arctozolt" to 881,
        "dracovish" to 882, "arctovish" to 883,
        "duraludon" to 884,
        "dreepy" to 885, "drakloak" to 886, "dragapult" to 887,
        "zacian" to 888, "zamazenta" to 889, "eternatus" to 890,
        "kubfu" to 891, "urshifu" to 892,
        "zarude" to 893,
        "regieleki" to 894, "regidrago" to 895,
        "glastrier" to 896, "spectrier" to 897, "calyrex" to 898,
        "wyrdeer" to 899, "kleavor" to 900,
        "ursaluna" to 901, "basculegion" to 902,
        "sneasler" to 903, "overqwil" to 904, "enamorus" to 905,

        // ── Gen 9 ──────────────────────────────────────────
        "sprigatito" to 906, "floragato" to 907, "meowscarada" to 908,
        "fuecoco" to 909, "crocalor" to 910, "skeledirge" to 911,
        "quaxly" to 912, "quaxwell" to 913, "quaquaval" to 914,
        "lechonk" to 915, "oinkologne" to 916,
        "tarountula" to 917, "spidops" to 918,
        "nymble" to 919, "lokix" to 920,
        "pawmi" to 921, "pawmo" to 922, "pawmot" to 923,
        "tandemaus" to 924, "maushold" to 925,
        "fidough" to 926, "dachsbun" to 927,
        "smoliv" to 928, "dolliv" to 929, "arboliva" to 930,
        "squawkabilly" to 931,
        "nacli" to 932, "naclstack" to 933, "garganacl" to 934,
        "charcadet" to 935, "armarouge" to 936, "ceruledge" to 937,
        "tadbulb" to 938, "bellibolt" to 939,
        "wattrel" to 940, "kilowattrel" to 941,
        "maschiff" to 942, "mabosstiff" to 943,
        "shroodle" to 944, "grafaiai" to 945,
        "bramblin" to 946, "brambleghast" to 947,
        "toedscool" to 948, "toedscruel" to 949,
        "klawf" to 950,
        "capsakid" to 951, "scovillain" to 952,
        "rellor" to 953, "rabsca" to 954,
        "flittle" to 955, "espathra" to 956,
        "tinkatink" to 957, "tinkatuff" to 958, "tinkaton" to 959,
        "wiglett" to 960, "wugtrio" to 961,
        "bombirdier" to 962,
        "finizen" to 963, "palafin" to 964,
        "varoom" to 965, "revavroom" to 966,
        "cyclizar" to 967, "orthworm" to 968,
        "glimmet" to 969, "glimmora" to 970,
        "greavard" to 971, "houndstone" to 972,
        "flamigo" to 973,
        "cetoddle" to 974, "cetitan" to 975,
        "veluza" to 976, "dondozo" to 977, "tatsugiri" to 978,
        "annihilape" to 979, "clodsire" to 980,
        "farigiraf" to 981, "dudunsparce" to 982,
        "kingambit" to 983,
        "great tusk" to 984, "scream tail" to 985,
        "brute bonnet" to 986, "flutter mane" to 987,
        "slither wing" to 988, "sandy shocks" to 989,
        "iron treads" to 990, "iron bundle" to 991,
        "iron hands" to 992, "iron jugulis" to 993,
        "iron moth" to 994, "iron thorns" to 995,
        "frigibax" to 996, "arctibax" to 997, "baxcalibur" to 998,
        "gimmighoul" to 999, "gholdengo" to 1000,
        "wo-chien" to 1001, "wo chien" to 1001,
        "chien-pao" to 1002, "chien pao" to 1002,
        "ting-lu" to 1003, "ting lu" to 1003,
        "chi-yu" to 1004, "chi yu" to 1004,
        "roaring moon" to 1005, "iron valiant" to 1006,
        "koraidon" to 1007, "miraidon" to 1008,
        "walking wake" to 1009, "iron leaves" to 1010,
        "dipplin" to 1011,
        "poltchageist" to 1012, "sinistcha" to 1013,
        "okidogi" to 1014, "munkidori" to 1015, "fezandipiti" to 1016,
        "ogerpon" to 1017, "archaludon" to 1018, "hydrapple" to 1019,
        "gouging fire" to 1020, "raging bolt" to 1021,
        "iron boulder" to 1022, "iron crown" to 1023,
        "terapagos" to 1024, "pecharunt" to 1025,
    )

    // Regex to strip any remaining possessive prefix like "N's ", "Marnie's ", "Cynthia's "
    // Runs after the explicit prefix list so multi-word owners (e.g. "Team Rocket's") are already removed.
    private val possessiveRegex = Regex("""(?i)\b[A-Za-z][A-Za-z0-9.]*'s\s+""")

    // Regex to split Tag Team / LEGEND card names on " & " or " and "
    private val tagTeamSplitRegex = Regex("""\s+(?:&|and)\s+""", RegexOption.IGNORE_CASE)

    private val moshi = com.squareup.moshi.Moshi.Builder().build()
    private val listIntAdapter = moshi.adapter<List<Int>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, Int::class.javaObjectType))

    /**
     * Parses a JSON string of Dex IDs or returns a list containing the single dexId if provided.
     */
    fun parseDexIds(dexIdsJson: String?, singleDexId: String?): List<Int> {
        return try {
            dexIdsJson?.let { listIntAdapter.fromJson(it) } ?: listOfNotNull(singleDexId?.toIntOrNull())
        } catch (_: Exception) {
            listOfNotNull(singleDexId?.toIntOrNull())
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns the primary (first) Pokémon base name extracted from [fullName].
     * Handles possessive prefixes ("N's Zekrom" → "Zekrom"), card mechanic
     * suffixes ("VMAX", "EX", "ex", "Tera", etc.), regional forms ("Galarian",
     * "Hisuian", etc.), Mega/Primal, and Platinum-era NPC tags ("FB", "GL").
     */
    fun extractPokemonName(fullName: String): String {
        return extractAllPokemonNames(fullName).firstOrNull()
            ?: fullName.split(" ").first()
    }

    /**
     * Splits Tag Team / LEGEND card names and returns a cleaned base name for
     * every Pokémon on the card.
     * e.g. "Pikachu & Zekrom-GX"  → ["Pikachu", "Zekrom"]
     * e.g. "N's Zekrom"           → ["Zekrom"]
     * e.g. "Darkrai & Cresselia LEGEND" → ["Darkrai", "Cresselia"]
     */
    fun extractAllPokemonNames(fullName: String): List<String> {
        return fullName.split(tagTeamSplitRegex)
            .map { cleanSinglePokemonName(it.trim()) }
            .filter { it.length >= 2 }
    }

    /**
     * Looks up the National Dex number for a single cleaned Pokémon base name.
     * Case-insensitive. Returns null if the name is not in the static map.
     */
    fun lookupDexId(pokemonName: String): Int? =
        POKEMON_DEX_MAP[pokemonName.trim().lowercase()]

    /**
     * Given any full card name (possessive variant, Tag Team, mechanic suffix, etc.),
     * extracts all Pokémon names and returns their National Dex numbers.
     * e.g. "Pikachu & Zekrom-GX" → [25, 644]
     * e.g. "N's Zekrom"          → [644]
     */
    fun lookupDexIds(fullName: String): List<Int> =
        extractAllPokemonNames(fullName).mapNotNull { lookupDexId(it) }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private fun cleanSinglePokemonName(input: String): String {
        var name = input

        // 1. Remove card-mechanic suffixes (order matters: longest first where ambiguous)
        val suffixes = listOf(
            // Modern mechanics
            "VMAX", "VSTAR", "V-UNION", "V-MAX", "V",
            "GX", "EX", "ex",
            "BREAK",
            "Prism Star", "◇", "♦",
            "Prime", "LEGEND",
            "Tag Team",
            "Star",
            "LV.X", "Level X",
            // Forms / eras listed as suffixes on some cards
            "Radiant",
            "Tera",
            "Rapid Strike", "Single Strike", "Fusion Strike",
            "Ancient", "Future",
            "Delta Species",
            "Origin Forme", "Altered Forme",
            "Holon's",
            // Platinum-era NPC ownership tags that appear after the name
            "FB", "GL", "C", "G",
        )
        suffixes.forEach { suffix ->
            name = name.replace(Regex("(?i)\\b${Regex.escape(suffix)}\\b"), "")
        }

        // 2. Remove known owner/form prefixes (explicit list, handles multi-word entries)
        val prefixes = listOf(
            // Gym Leaders / Elite Four (Gen 1)
            "Team Rocket's", "Brock's", "Misty's", "Lt. Surge's", "Erika's",
            "Koga's", "Sabrina's", "Blaine's", "Giovanni's",
            // Card states / alignments
            "Dark", "Light", "Shining", "Shadow", "Armored", "Radiant",
            // Regional forms
            "Alolan", "Galarian", "Hisuian", "Paldean",
            // Forme variants (as prefixes on some printings)
            "Origin Forme", "Therian Forme", "Altered Forme",
            // Mega / Primal
            "Mega", "Primal",
            // Battle-style tags (also appear as prefixes on some printings)
            "Rapid Strike", "Single Strike", "Fusion Strike",
            // Generation markers
            "Ancient", "Future",
        )
        prefixes.forEach { prefix ->
            val escapedPrefix = Regex.escape(prefix)
            val pattern = if (prefix.contains("'")) {
                Regex("(?i)\\b$escapedPrefix\\s*")
            } else {
                Regex("(?i)\\b$escapedPrefix\\b\\s*")
            }
            name = name.replace(pattern, "")
        }

        // 3. Generic possessive: strips any remaining "Xyz's " pattern
        //    Covers N's, Marnie's, Cynthia's, Colress's, Lusamine's, etc.
        name = name.replace(possessiveRegex, "")

        // 4. Handle bare "M " prefix for Mega Pokémon (e.g. "M Charizard EX")
        name = name.replace(Regex("^M\\s", RegexOption.IGNORE_CASE), "")

        // 5. Strip parenthetical sub-names / variant labels, trim whitespace
        name = name.split("(", "[", "{").first()
            .trim()
            .replace(Regex("\\s+"), " ")

        // Fallback: if cleaning produced something too short, return first word of input
        return if (name.length < 2) input.split(" ").first() else name
    }
}
