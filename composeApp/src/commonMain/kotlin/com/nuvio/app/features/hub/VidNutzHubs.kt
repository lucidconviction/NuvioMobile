package com.nuvio.app.features.hub

/** Static VidNutz hub catalog (hub -> sub-collection rails). */
object VidNutzHubs {

    fun sub(name: String, count: Int): VidNutzSub {
        val id = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return VidNutzSub(id = id, name = name, count = count, queries = listOf(name))
    }

    val all: List<VidNutzHub> = listOf(
        VidNutzHub("always_on", "24/7 & Live", listOf(
            sub("24/7 Live Cams & Safari", 203), sub("24/7 News Live", 62),
            sub("Music 24/7 & Long Mixes", 47), sub("Fireplace, Rain, Nature & Sleep Loops", 466),
        )),
        VidNutzHub("pred_watch", "Pred Watch", listOf(
            VidNutzSub("pp-southeast-texas", "PP Southeast Texas", 0, listOf("PP Southeast Texas predator", "Alex Rosen pred", "PP Southeast Texas catches", "NVCAP")),
            VidNutzSub("nvcap", "NVCAP", 0, listOf("NVCAP", "Non-violent citizens against predators")),
            VidNutzSub("colorado-ped-patrol", "Colorado Ped Patrol", 0, listOf("Colorado Ped Patrol predator", "Colorado Ped Patrol sting")),
            VidNutzSub("trilogy-media", "Trilogy Media", 0, listOf("Trilogy Media predator sting", "Trilogy Media pred")),
            VidNutzSub("skeeter-jean", "Skeeter Jean", 0, listOf("Skeeter Jean", "therealskeeterjean", "Skeeter Jean predator")),
            VidNutzSub("omma", "Omma", 0, listOf("Omma predator", "Omma catches predators")),
            VidNutzSub("jidion", "JiDion", 0, listOf("JiDion predator", "JiDion pred sting")),
            VidNutzSub("the-zerg", "The Zerg", 0, listOf("TheMrZerg", "The Zerg predator", "The Zerg sting")),
            VidNutzSub("schlep", "Schlep", 0, listOf("RealSchlep", "Schlep predator", "Schlep sting")),
            VidNutzSub("cop", "C.O.P - Catching Online Predators", 0, listOf("C.O.P Catching Online Predators", "C.O.P predator sting")),
            VidNutzSub("luan-lennon", "Luan Lennon", 0, listOf("Luan Lennon predator", "Luan Lennon sting")),
            VidNutzSub("have-a-seat", "Have a Seat With Chris Hansen", 0, listOf("Have a Seat With Chris Hansen", "Chris Hansen predator sting")),
        )),
        VidNutzHub("around_the_world", "Around the World", listOf(
            sub("India", 119), sub("Turkiye", 94), sub("Latin America", 10), sub("Africa", 76),
        )),
        VidNutzHub("big_talk", "Big Talk", listOf(
            sub("Long Interviews", 147), sub("Science & Health", 390), sub("History & War Talks", 150),
            sub("Sport & Fight Talk", 119), sub("Business & Money", 97),
        )),
        VidNutzHub("cars_motoring", "Cars & Motoring", listOf(
            sub("Barn Finds", 228), sub("Junkyard Revivals", 237), sub("Off-Road Rescue", 219),
            sub("Full Restorations", 235), sub("Project Builds", 424), sub("Road Trip Specials", 115),
            sub("Workshop Diaries", 155), sub("Classic Car Stories", 284), sub("Full Episodes & Long Builds", 222),
            sub("Road Trips & Reviews", 195),
        )),
        VidNutzHub("comedy", "Comedy", listOf(
            sub("Funny Home Videos", 11), sub("AFV Classics", 11), sub("Pranks", 81), sub("Stand-up", 77),
            sub("Comedy Central", 48), sub("Taskmaster", 80), sub("Graham Norton", 84), sub("Fails", 38),
            sub("Stand-Up & Hidden Camera", 36), sub("Ridiculousness & Stunts", 105), sub("Stunts & Big Builds", 146),
            sub("Challenge Shows", 28), sub("Pranks & Hidden Camera", 242), sub("Full Stand-Up Specials", 146),
            sub("Stand-Up Showcases", 212), sub("Gabriel Iglesias", 34), sub("Trevor Noah", 16), sub("Matt Rife", 95),
            sub("Andrew Schulz", 31), sub("Jimmy Carr", 71), sub("Kevin Hart & LOL Network", 39),
            sub("British Stand-Up", 164), sub("British Comedy Legends", 351), sub("More Stand-Up Comedians", 341),
            sub("Kiwi & Aussie Comedy", 263), sub("Classic Comedy", 177), sub("Sitcom Moments", 334),
            sub("Sketch Comedy", 328), sub("Stand-Up from India", 35), sub("African Comedy", 138), sub("World Comedy", 155),
        )),
        VidNutzHub("cooking_food", "Cooking & Food", listOf(
            sub("Cooking Competition - Full Episodes & Marathons", 111), sub("Cook Along & Food Science", 226),
            sub("Full Cooking Shows", 128), sub("Kitchens of the World", 236), sub("BBQ & Baking", 214),
            sub("Chefs", 143), sub("Baking & Desserts", 54), sub("Food Science & Technique", 90),
        )),
        VidNutzHub("crime", "Crime", listOf(
            sub("COPS", 48), sub("True Crime", 99), sub("Police", 93), sub("Courtroom", 99), sub("Live Trials", 98),
            sub("Investigations", 100), sub("Murder Cases", 83), sub("Cold Cases", 177), sub("Prisons", 233),
            sub("Emergency Services", 310), sub("Police Chases", 295), sub("Forensics", 161), sub("Scams & Fraud", 442),
            sub("True Crime Full Episodes", 95),
        )),
        VidNutzHub("documentaries", "Documentaries", listOf(
            sub("Featured Documentaries", 165), sub("Nature", 176), sub("History", 188), sub("Science", 81),
            sub("Space", 78), sub("Engineering", 503), sub("War & Military", 365), sub("Travel", 152),
            sub("Wildlife", 12), sub("People & Society", 454), sub("Investigations", 22), sub("Technology", 361),
            sub("Disaster", 500), sub("Archaeology", 360), sub("Oceans", 300), sub("Aviation", 434),
        )),
        VidNutzHub("free_movies", "Free Movies", listOf(
            sub("Westerns", 168), sub("Sci-Fi & Fantasy", 142), sub("Family Films", 133), sub("Martial Arts", 39),
        )),
        VidNutzHub("game_shows", "Game Shows", listOf(
            sub("Deal or No Deal", 71), sub("Family Feud", 70), sub("Wheel of Fortune", 15), sub("Jeopardy!", 65),
            sub("The Price Is Right", 99), sub("Let's Make a Deal", 100), sub("Match Game", 7), sub("Taskmaster", 80),
            sub("The Moment of Truth", 92), sub("Classic Game Shows", 192), sub("The Chase", 71), sub("25 Words or Less", 44),
            sub("Game Show Network Originals", 41), sub("QI", 94), sub("Pointless", 85), sub("Would I Lie To You?", 83),
            sub("Tipping Point", 84), sub("Who Wants to Be a Millionaire?", 179), sub("Mock The Week", 80),
            sub("8 Out of 10 Cats", 66), sub("The Big Fat Quiz", 100), sub("The Cube", 146),
            sub("Richard Osman's House of Games", 56), sub("Catchphrase", 60), sub("More Game Shows", 144),
            sub("The Traitors", 148), sub("Got Talent", 266), sub("Singing Contests", 53), sub("Dance Contests", 1),
            sub("Singing & Dance Formats", 170),
        )),
        VidNutzHub("gaming", "Gaming", listOf(
            sub("Minecraft", 355), sub("Roblox & Family Gaming", 74), sub("Fortnite & Battle Royale", 97),
            sub("Retro, Speedrunning & Game Docs", 168), sub("Official Esports & Directs", 100),
            sub("Gaming Legends", 41), sub("Horror & Scary Games", 85), sub("Mature Docs & Showcases", 54),
        )),
        VidNutzHub("health_and_wellbeing", "Health & Wellbeing", listOf(
            sub("Walking & Workouts", 170), sub("Stretch, Recover & Relax", 200),
        )),
        VidNutzHub("history", "History", listOf(
            sub("Full-Length & Feature-Length Documentaries", 153), sub("Living History", 150),
            sub("Ancient & Medieval", 64), sub("War & Military History", 137),
        )),
        VidNutzHub("influencers", "Influencers", listOf(
            sub("MrBeast Universe", 69), sub("The MrBeast Crew", 6), sub("Big-Budget Challenges", 42),
            sub("Family Challenge Squad", 35), sub("Lexi Rivera & the Amp Crew", 155), sub("Big Family Channels", 119),
            sub("Family Creators Worldwide", 194), sub("Challenge Crews (Parental Gate)", 65),
        )),
        VidNutzHub("kids", "Kids", listOf(
            sub("Preschool Favourites", 689), sub("Bedtime & Calm", 245), sub("Songs & Rhymes", 253),
            sub("Numbers, Letters & Colours", 215), sub("Vintage Preschool Classics", 609),
            sub("Classic British Kids TV", 405), sub("Stop-Motion Favourites", 88), sub("Golden Age Cartoons", 314),
            sub("Saturday Morning Cartoons", 152), sub("Cartoon Network & Nickelodeon", 348),
            sub("Disney & Magical Adventures", 268), sub("Action Heroes & Building Worlds", 121),
            sub("Animals & The Wild", 298), sub("Learn, Make & Discover", 542), sub("British Kids TV Today", 252),
            sub("Aussie & Kiwi Kids TV", 102),
        )),
        VidNutzHub("live_cams", "Live Cams", listOf(
            sub("Wildlife", 30), sub("Birds", 136), sub("Nests", 117), sub("Bears", 34), sub("Dog Parks", 201),
            sub("Skateparks", 3), sub("Beaches", 311), sub("Railways", 93), sub("Volcanoes", 35), sub("Weather", 107),
            sub("Planespotting", 195), sub("Scenic", 38),
        )),
        VidNutzHub("makers_and_builders", "Makers & Builders", listOf(
            sub("Workshop & Making", 140), sub("Restoration & Forging", 168),
            sub("Woodwork, 3D Printing & Models", 121), sub("Home Renovation", 132),
        )),
        VidNutzHub("music_concerts", "Music & Concerts", listOf(
            sub("Full Concerts", 87), sub("Festival Headline Sets", 36), sub("DJ Sets in Amazing Places", 134),
            sub("Tiny Desk", 232), sub("Live in the Studio", 194), sub("Classic Rock Live", 24),
            sub("Reggae & Caribbean Live", 30),
        )),
        VidNutzHub("nature_wild", "Nature & Wild", listOf(
            sub("Big Cats", 201), sub("Oceans & Sharks", 314), sub("Africa", 96), sub("Wild Rescues", 72),
            sub("4K Nature Ambience", 405), sub("Survival in the Wild", 455), sub("Birds & Backyard", 207),
            sub("Feature Wildlife Films", 114),
        )),
        VidNutzHub("real_life_tv", "Real Life TV", listOf(
            sub("MasterChef World", 251), sub("Property & Renovation", 85), sub("Airports & Borders", 311),
            sub("Extreme Jobs", 511), sub("Life at Sea", 494), sub("Daytime Court", 313),
            sub("Reality & Deals - Full Episodes", 33), sub("Long-Form Factual & Marathons", 43), sub("Big Brother", 62),
            sub("Shore & Party Reality", 101), sub("MTV Reality Marathons", 161),
            sub("Reality Ensembles & Dating Drama", 170), sub("Celebrity & Family Reality", 343),
        )),
        VidNutzHub("science_and_spectacle", "Science & Spectacle", listOf(
            sub("Stunts & Challenges", 117), sub("Science & Curiosity", 284),
        )),
        VidNutzHub("short_stories", "Short Stories", listOf(
            sub("Dhar Mann & Life Lessons", 70), sub("Scripted Shorts & Series", 4),
            sub("Storytime & True Tales", 7), sub("Short Films (Parental Gate)", 207), sub("After Dark", 161),
            sub("Dhar Mann & Moral Stories", 117),
        )),
        VidNutzHub("tech_and_gear", "Tech & Gear", listOf(
            sub("Tech Reviews", 134),
        )),
        VidNutzHub("ufo", "UFO & Space", listOf(
            sub("UFO / UAP", 21), sub("Space Mysteries", 462), sub("Government / Official Material", 93),
            sub("Alien & UFO Documentaries", 211), sub("Ancient Mysteries", 25), sub("Paranormal", 433),
            sub("Unexplained Events", 365), sub("Astronomy", 451), sub("NASA", 83), sub("Space", 75),
            sub("UFOs & The Unexplained", 4),
        )),
        VidNutzHub("walking_tours", "Walking Tours", listOf(
            sub("4K City Walks", 291), sub("Night Walks", 188), sub("Beaches & Coastlines", 225),
            sub("Japan & Asia", 324), sub("Europe on Foot", 286), sub("Rain & Ambient Walks", 92),
            sub("Train Cab Rides", 158), sub("Scenic Drives", 483), sub("4K Walking Tours", 224),
            sub("Cab Rides, Road & Water Journeys", 267), sub("Walk the World", 944), sub("Night & Rain Walks", 92),
            sub("Cab Rides & Scenic Drives", 115),
        )),
        VidNutzHub("nett_smutt", "Nett Smutt", listOf(
            sub("Crazyshit", 30), sub("DaftPorn", 30), sub("eFukt", 30),
            sub("HorribleVideos", 30), sub("Inhumanity", 30), sub("Kaotic", 30),
            sub("LiveGore", 30), sub("NothingToxic", 30), sub("TheYNC", 30),
            sub("USACrime", 30), sub("Worldstar", 30), sub("YouPorn", 30),
            sub("Z-Filmz Originals", 30),
        )),
        VidNutzHub("rumble_vids", "Rumble Vids", listOf(
            sub("Rumble News", 30), sub("Rumble Politics", 30), sub("Rumble Sports", 30),
            sub("Rumble Entertainment", 30), sub("Rumble Viral", 30), sub("Rumble Cooking", 30),
        )),
    )

    // Special hubs that consolidate the legacy flat categories.
    val trending = VidNutzHub("trending", "Trending", listOf(
        sub("Trending Now", 0), sub("Popular This Week", 0),
    ))
    val liveStreams = VidNutzHub("live_streams", "Live Streams", listOf(
        sub("Live Streams Now", 0), sub("24/7 Live TV", 0),
    ))

    val menu: List<VidNutzHub> = listOf(trending, liveStreams) + all

    /** Built-in default catalog used as the base for user edits (excludes Staff Picks). */
    val defaultsForEditing: List<VidNutzHub> = listOf(trending, liveStreams) + all

    fun byId(id: String): VidNutzHub? = menu.firstOrNull { it.id == id }
}