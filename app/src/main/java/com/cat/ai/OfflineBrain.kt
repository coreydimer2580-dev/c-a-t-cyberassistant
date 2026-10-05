package com.cat.ai

import com.cat.tools.LocalTools
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * v1.15 offline reasoning layer. Pure Kotlin, on-device, no network, no shell.
 * Recognises common intents (maths, conversions, dates, planning, decisions,
 * how-to, explain, ideas, drafting, feelings, follow-ups) and writes a
 * structured English answer. Returns null when it has nothing useful to add.
 */
object OfflineBrain {
    private val AU = Locale.forLanguageTag("en-AU")
    private const val HOME_ZONE = "Australia/Perth"

    data class Answer(val text: String, val intent: String)

    fun answer(
        clean: String,
        recentChat: List<Pair<String, String>>,
        todos: List<String> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): Answer? {
        val text = clean.trim()
        if (text.isEmpty()) return null
        val lower = text.lowercase(AU).trimEnd('?', '!', '.', ' ')

        followUp(lower, recentChat, todos, nowMillis)?.let { return it }
        conversion(lower)?.let { return Answer(it, "convert") }
        percent(lower)?.let { return Answer(it, "math") }
        math(lower)?.let { return Answer(it, "math") }
        dateAnswer(lower, nowMillis)?.let { return Answer(it, "date") }
        thanksOrBye(lower)?.let { return Answer(it, "social") }
        feelings(lower)?.let { return Answer(it, "feelings") }
        joke(lower)?.let { return Answer(it, "joke") }
        planDay(lower, todos)?.let { return Answer(it, "plan") }
        decision(lower)?.let { return Answer(it, "decide") }
        draft(lower, text)?.let { return Answer(it, "draft") }
        ideas(lower)?.let { return Answer(it, "ideas") }
        explain(lower)?.let { return Answer(it, "explain") }
        howTo(lower)?.let { return Answer(it, "howto") }
        compare(lower)?.let { return Answer(it, "compare") }
        return null
    }

    // ---------- follow-ups ("more", "why", "another one") ----------

    private val MORE = Regex("^(more|go on|continue|keep going|tell me more|and|another|another one|again|next|expand|elaborate|explain more|why)$")

    private fun followUp(
        lower: String,
        chat: List<Pair<String, String>>,
        todos: List<String>,
        now: Long
    ): Answer? {
        if (!MORE.matches(lower)) return null
        val lastUser = chat.lastOrNull { it.first == "user" && !MORE.matches(it.second.lowercase(AU).trimEnd('?', '!', '.', ' ')) }
            ?.second ?: return Answer(
            "Happy to go further — what topic? Give me a few words and I'll lay out steps, options, or a plan.",
            "followup"
        )
        if (lower == "why") {
            val topic = topicOf(lastUser)
            return Answer(
                "Why it matters for $topic:\n" +
                    "1. It saves time — a clear plan avoids redoing work.\n" +
                    "2. It lowers risk — you see trade-offs before committing.\n" +
                    "3. It's repeatable — next time you start from what worked.\n" +
                    "Tell me what you're weighing and I'll make the reasons specific.",
                "followup"
            )
        }
        val again = answer(lastUser, emptyList(), todos, now)
        val base = again?.text ?: "On \"${lastUser.take(80)}\":"
        return Answer(
            base + "\n\nGoing further:\n" +
                "- Smallest next step: do one 10-minute piece of it today.\n" +
                "- What could go wrong: name it now and pick a fallback.\n" +
                "- How you'll know it worked: one clear sign to check.",
            "followup"
        )
    }

    // ---------- maths ----------

    private val WORD_OPS = listOf(
        Regex("\\bmultiplied by\\b") to "*",
        Regex("\\btimes\\b") to "*",
        Regex("\\bdivided by\\b") to "/",
        Regex("\\bover\\b") to "/",
        Regex("\\bplus\\b") to "+",
        Regex("\\bminus\\b") to "-",
        Regex("\\bsquared\\b") to "^2",
        Regex("\\bcubed\\b") to "^3",
        Regex("\\bto the power of\\b") to "^",
        Regex("(?<=\\d)\\s*x\\s*(?=\\d)") to "*",
        Regex("×") to "*",
        Regex("÷") to "/"
    )

    private fun math(lower: String): String? {
        var expr = lower
            .replace(Regex("^(what is|what's|whats|calculate|calc|compute|work out|solve|how much is)\\s+"), "")
            .replace("=", "")
            .replace(",", "")
            .trim()
        WORD_OPS.forEach { (re, op) -> expr = expr.replace(re, op) }
        expr = expr.replace(Regex("\\bsqrt\\s*(\\d+(?:\\.\\d+)?)"), "sqrt($1)")
        if (!Regex("^[0-9+\\-*/^().\\s sqrt]+$").matches(expr)) return null
        if (!Regex("\\d").containsMatchIn(expr)) return null
        if (!Regex("[+\\-*/^]|sqrt").containsMatchIn(expr.drop(1))) return null
        val value = runCatching { Expr(expr.replace(" ", "")).parse() }.getOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return "That works out to an undefined number (dividing by zero?)."
        return "${expr.replace(" ", "")} = ${fmt(value)}"
    }

    private fun percent(lower: String): String? {
        Regex("(\\d+(?:\\.\\d+)?)\\s*%\\s*of\\s*\\$?(\\d+(?:\\.\\d+)?)").find(lower)?.let { m ->
            val p = m.groupValues[1].toDouble()
            val n = m.groupValues[2].toDouble()
            return "${fmt(p)}% of ${fmt(n)} = ${fmt(p * n / 100.0)}"
        }
        Regex("(?:tip|gst)\\D*\\$?(\\d+(?:\\.\\d+)?)").find(lower)?.let { m ->
            val n = m.groupValues[1].toDouble()
            return if (lower.contains("gst")) {
                "GST (10%) on \$${fmt(n)}: \$${fmt(n * 0.1)} · total \$${fmt(n * 1.1)} · " +
                    "if \$${fmt(n)} already includes GST, the GST part is \$${fmt(n / 11.0)}."
            } else {
                "Tip on \$${fmt(n)}: 10% = \$${fmt(n * 0.10)}, 15% = \$${fmt(n * 0.15)}, 20% = \$${fmt(n * 0.20)}."
            }
        }
        return null
    }

    private class Expr(private val s: String) {
        private var i = 0
        fun parse(): Double {
            val v = sum()
            require(i == s.length)
            return v
        }
        private fun sum(): Double {
            var v = product()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]
                val r = product()
                v = if (op == '+') v + r else v - r
            }
            return v
        }
        private fun product(): Double {
            var v = power()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]
                val r = power()
                v = if (op == '*') v * r else v / r
            }
            return v
        }
        private fun power(): Double {
            val b = unary()
            if (i < s.length && s[i] == '^') {
                i++
                return Math.pow(b, power())
            }
            return b
        }
        private fun unary(): Double {
            if (i < s.length && s[i] == '-') { i++; return -unary() }
            if (i < s.length && s[i] == '+') { i++; return unary() }
            return atom()
        }
        private fun atom(): Double {
            if (s.startsWith("sqrt", i)) {
                i += 4
                return Math.sqrt(atom())
            }
            if (i < s.length && s[i] == '(') {
                i++
                val v = sum()
                require(i < s.length && s[i] == ')')
                i++
                return v
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(i > start)
            return s.substring(start, i).toDouble()
        }
    }

    // ---------- conversions ----------

    private val UNIT_ALIASES = mapOf(
        "kilometres" to "km", "kilometers" to "km", "kilometre" to "km", "kilometer" to "km",
        "metres" to "m", "meters" to "m", "metre" to "m", "meter" to "m",
        "centimetres" to "cm", "centimeters" to "cm", "millimetres" to "mm", "millimeters" to "mm",
        "miles" to "mi", "mile" to "mi", "feet" to "ft", "foot" to "ft", "inches" to "in", "inch" to "in",
        "kilograms" to "kg", "kilos" to "kg", "kilo" to "kg", "grams" to "g", "gram" to "g",
        "pounds" to "lb", "pound" to "lb", "lbs" to "lb", "ounces" to "oz", "ounce" to "oz",
        "celsius" to "c", "fahrenheit" to "f", "°c" to "c", "°f" to "f", "degrees c" to "c", "degrees f" to "f"
    )

    private fun unit(raw: String): String = UNIT_ALIASES[raw.trim()] ?: raw.trim()

    private fun conversion(lower: String): String? {
        val m = Regex("(-?\\d+(?:\\.\\d+)?)\\s*([a-z°]+(?: [cf])?)\\s+(?:to|in|into|as)\\s+([a-z°]+)").find(lower)
            ?: return null
        val value = m.groupValues[1].toDouble()
        val from = unit(m.groupValues[2])
        val to = unit(m.groupValues[3])
        val out = LocalTools.convert(value, from, to) ?: return null
        return "${fmt(value)} $from = ${LocalTools.formatAmount(out)} $to"
    }

    // ---------- dates ----------

    private fun dateAnswer(lower: String, now: Long): String? {
        val zone = ZoneId.of(HOME_ZONE)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val dayFmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", AU)
        return when {
            Regex("(what day is it|what's the date|whats the date|what is the date|today's date|todays date|date today)").containsMatchIn(lower) ->
                "Today in Perth is ${today.format(dayFmt)}."
            lower.contains("tomorrow") && Regex("(what day|date)").containsMatchIn(lower) ->
                "Tomorrow in Perth is ${today.plusDays(1).format(dayFmt)}."
            else -> {
                val m = Regex("how many days (?:until|till|to) (christmas|new year|easter)").find(lower) ?: return null
                val target = when (m.groupValues[1]) {
                    "christmas" -> today.withMonth(12).withDayOfMonth(25)
                    "new year" -> today.plusYears(1).withDayOfYear(1)
                    else -> return null
                }.let { if (it.isBefore(today)) it.plusYears(1) else it }
                "${ChronoUnit.DAYS.between(today, target)} days until ${m.groupValues[1]} (${target.format(dayFmt)}, Perth)."
            }
        }
    }

    // ---------- social / feelings / fun ----------

    private fun thanksOrBye(lower: String): String? = when {
        Regex("^(thanks|thank you|thx|cheers|ta|legend)\\b").containsMatchIn(lower) ->
            "Anytime. Want me to save anything from this, or keep going?"
        Regex("^(bye|goodbye|see ya|cya|later|good night|night)\\b").containsMatchIn(lower) ->
            "See you. Your notes stay on this phone."
        Regex("^(how are you|how's it going|hows it going|you good)").containsMatchIn(lower) ->
            "Running well, offline and ready. What are we working on?"
        else -> null
    }

    private fun feelings(lower: String): String? {
        val m = Regex("\\b(?:i'?m|i am|feeling|i feel)\\s+(?:so |really |very |a bit )?(stressed|anxious|sad|tired|bored|overwhelmed|angry|lonely|stuck|unmotivated|exhausted|worried)\\b")
            .find(lower) ?: return null
        val mood = m.groupValues[1]
        val steps = when (mood) {
            "tired", "exhausted" -> listOf("Drink water and eat something simple.", "Do only the one thing that can't wait.", "Plan a proper break or an early night.")
            "bored", "unmotivated", "stuck" -> listOf("Pick a 10-minute task — starting is the hard part.", "Change place or put on music.", "Tell me the goal and I'll break it into tiny steps.")
            "angry" -> listOf("Pause before replying to anyone.", "Write what happened in one sentence.", "Decide what outcome you actually want.")
            "lonely", "sad" -> listOf("Message one person you trust — even a short hi.", "Get outside for a few minutes.", "Be kind to yourself; this feeling passes.")
            else -> listOf("Breathe slowly: in 4, hold 4, out 6 — five times.", "Write down everything on your mind, then circle one thing.", "Do that one thing for 10 minutes.")
        }
        return "Sorry you're feeling $mood. A few small things that help:\n" +
            steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n") +
            "\nIf it ever feels like too much, Lifeline is 13 11 14 (Australia, 24/7)."
    }

    private val JOKES = listOf(
        "Why did the phone wear glasses? It lost its contacts.",
        "I told my Wi-Fi a joke. It didn't get it — weak signal.",
        "Why do programmers prefer dark mode? Light attracts bugs.",
        "Why was the computer cold? It left its Windows open."
    )

    private fun joke(lower: String): String? {
        if (!Regex("\\b(joke|make me laugh|something funny)\\b").containsMatchIn(lower)) return null
        return JOKES[(lower.hashCode() and 0x7fffffff) % JOKES.size]
    }

    // ---------- planning / decisions / how-to ----------

    private fun planDay(lower: String, todos: List<String>): String? {
        if (!Regex("\\b(plan (my|the|a) (day|morning|week|evening)|schedule my|what should i do today|organi[sz]e my day)\\b").containsMatchIn(lower)) return null
        val open = todos.filter { it.startsWith("open:") }.map { it.removePrefix("open:").trim() }
        val tasks = if (open.isNotEmpty()) open.take(4) else listOf("your most important task", "admin / messages", "a smaller task", "something for you")
        val source = if (open.isNotEmpty()) "from your checklist" else "add real items with /todo to personalise this"
        return "Here's a simple plan ($source):\n" +
            "1. Morning focus (60–90 min): ${tasks[0]}\n" +
            "2. Short break — water, stretch.\n" +
            "3. Late morning: ${tasks.getOrElse(1) { "admin / messages" }}\n" +
            "4. Lunch away from the screen.\n" +
            "5. Afternoon: ${tasks.getOrElse(2) { "a smaller task" }}\n" +
            "6. Wrap-up (15 min): tick off what's done, pick tomorrow's first task.\n" +
            "7. Evening: ${tasks.getOrElse(3) { "something for you" }}\n" +
            "Tip: do the hardest thing first, while your energy is highest."
    }

    private fun decision(lower: String): String? {
        val either = Regex("^(?:should i|do i|would it be better to|is it better to)\\s+(.+?)\\s+or\\s+(.+)$").find(lower)
        if (either != null) {
            val a = either.groupValues[1].trim()
            val b = either.groupValues[2].trim()
            return "Choosing between \"$a\" and \"$b\":\n" +
                "1. What's the goal? Pick the option that moves it most.\n" +
                "2. Cost: which takes less time, money, or energy?\n" +
                "3. Risk: which is easier to undo if it goes wrong?\n" +
                "4. Regret test: in a month, which would you wish you'd picked?\n" +
                "Quick rule: if they're close, pick the reversible one and start.\n" +
                "Tell me what matters most (cost, time, fun, safety) and I'll weigh them."
        }
        val single = Regex("^(?:should i|is it worth|is it a good idea to)\\s+(.+)$").find(lower) ?: return null
        val what = single.groupValues[1].trim()
        return "Should you $what? Quick check:\n" +
            "- Upside: what do you gain if it goes well?\n" +
            "- Downside: what's the worst realistic outcome — can you live with it?\n" +
            "- Timing: is now better than later?\n" +
            "- Values: does it fit what you care about?\n" +
            "If the upside is clear and the downside is survivable, lean yes. Give me the details and I'll be more specific."
    }

    private fun howTo(lower: String): String? {
        val m = Regex("^(?:how (?:do|can|should) i|how to|how would i|what's the best way to|whats the best way to|best way to|help me)\\s+(.+)$").find(lower)
            ?: return null
        val task = m.groupValues[1].trim()
        known(task)?.let { return it }
        return "How to $task:\n" +
            "1. Define done — what does \"finished\" look like?\n" +
            "2. Gather what you need (info, tools, people, time).\n" +
            "3. Break it into 3–5 small steps; start with the easiest.\n" +
            "4. Do step 1 now, then check it worked before moving on.\n" +
            "5. Review: what would you do differently next time?\n" +
            "Tell me more about your situation and I'll make these steps specific."
    }

    private fun known(task: String): String? = when {
        task.contains("save money") || task.contains("budget") ->
            "Budget in 4 steps:\n1. List income after tax.\n2. Track every spend for 2 weeks.\n3. Try 50/30/20: needs / wants / savings.\n4. Automate savings on payday.\nBonus: cancel one unused subscription today."
        task.contains("sleep") ->
            "Better sleep:\n1. Same wake time every day, weekends too.\n2. No screens 30–60 min before bed.\n3. Cool, dark, quiet room.\n4. No caffeine after early afternoon.\n5. If you can't sleep in 20 min, get up and read until sleepy."
        task.contains("focus") || task.contains("concentrate") || task.contains("procrastinat") ->
            "Focus tactics:\n1. One task, written down.\n2. Phone in another room.\n3. 25 minutes on, 5 off (Pomodoro).\n4. Start with a 2-minute version of the task.\n5. Reward yourself after 4 rounds."
        task.contains("password") || task.contains("secure my") || task.contains("stay safe online") ->
            "Stay secure:\n1. Use a password manager and unique passphrases (try /pass 20).\n2. Turn on two-factor authentication for email and banking.\n3. Keep your phone updated.\n4. Don't tap links in unexpected texts — go to the site yourself.\n5. Back up your photos and files."
        task.contains("learn") && (task.contains("code") || task.contains("coding") || task.contains("program")) ->
            "Learning to code:\n1. Pick one language (Python is friendly).\n2. 30 minutes a day beats 5 hours once a week.\n3. Build tiny projects: a calculator, a to-do list.\n4. Read errors carefully — they tell you what's wrong.\n5. Share your code and ask for feedback."
        task.contains("battery") ->
            "Phone battery tips:\n1. Lower screen brightness or use auto.\n2. Turn on battery saver below 30%.\n3. Check Settings → Battery for hungry apps.\n4. Avoid heat; keep charge between 20–80% when you can."
        else -> null
    }

    // ---------- explain / glossary ----------

    private val GLOSSARY = mapOf(
        "ai" to "AI (artificial intelligence) is software that learns patterns from data to make predictions or generate text, images, or decisions.",
        "api" to "An API is a set of rules that lets one program ask another for data or actions — like a waiter taking your order to the kitchen.",
        "vpn" to "A VPN encrypts your internet traffic and routes it through another server, hiding it from the local network and changing your apparent location.",
        "cloud" to "\"The cloud\" means using someone else's computers over the internet to store files or run software, instead of your own device.",
        "encryption" to "Encryption scrambles data with a key so only someone with the right key can read it. C@T's Terminal vault uses AES-GCM.",
        "aes" to "AES is a widely used encryption standard. AES-GCM also detects tampering.",
        "phishing" to "Phishing is a scam message pretending to be a trusted company to trick you into giving passwords or money. Check the sender and never tap unexpected links.",
        "two factor" to "Two-factor authentication adds a second check (a code or app prompt) after your password, so a stolen password alone isn't enough.",
        "2fa" to "2FA (two-factor authentication) adds a second check after your password, so a stolen password alone isn't enough.",
        "ram" to "RAM is your device's short-term working memory — more RAM lets more apps stay open smoothly.",
        "cpu" to "The CPU is the processor — the part of the device that runs instructions.",
        "wifi" to "Wi-Fi is a wireless local network that connects your device to a router, which connects to the internet.",
        "bluetooth" to "Bluetooth is short-range wireless for headphones, watches, and nearby devices.",
        "inflation" to "Inflation is the general rise in prices over time, so the same money buys a bit less each year.",
        "interest" to "Interest is the cost of borrowing money (or the reward for saving it), usually a yearly percentage.",
        "compound interest" to "Compound interest is earning interest on your interest — growth speeds up the longer you leave it.",
        "superannuation" to "Super is Australia's retirement savings system: your employer pays a percentage of your wage into a fund you can access later in life.",
        "super" to "Super (superannuation) is Australia's retirement savings: your employer pays a share of your wage into a fund for later in life.",
        "gst" to "GST is Australia's 10% Goods and Services Tax on most things you buy. Ask me \"gst on 50\" to work it out.",
        "algorithm" to "An algorithm is a step-by-step recipe for solving a problem. C@T's path is one: YOU → SoftCorrect → MEMORY → REPLY → Evolve.",
        "blockchain" to "A blockchain is a shared, append-only record copied across many computers, so entries are hard to change after the fact.",
        "crypto" to "Cryptocurrency is digital money recorded on a blockchain. It's volatile — only risk what you can afford to lose.",
        "machine learning" to "Machine learning is a kind of AI where a program improves by finding patterns in examples instead of being told every rule.",
        "llm" to "An LLM (large language model) is AI trained on huge amounts of text to predict and write language — like ChatGPT.",
        "chatgpt" to "ChatGPT is OpenAI's cloud chatbot built on large language models. C@T's Online tab can use your own compatible endpoint, and answers offline otherwise.",
        "photosynthesis" to "Photosynthesis is how plants turn sunlight, water, and carbon dioxide into sugar for energy, releasing oxygen.",
        "gravity" to "Gravity is the pull between masses — it keeps us on Earth and the Moon orbiting.",
        "dna" to "DNA is the molecule carrying the genetic instructions for how living things grow and work.",
        "climate change" to "Climate change is the long-term warming of Earth, mainly from burning fossil fuels that trap heat in the atmosphere.",
        "black hole" to "A black hole is a region where gravity is so strong that nothing, not even light, can escape."
    )

    private fun explain(lower: String): String? {
        val m = Regex("^(?:what is|what's|whats|what are|explain|define|tell me about|meaning of|what does)\\s+(?:an? |the )?(.+?)(?:\\s+mean)?(?:\\s+simply| in simple terms| like i'?m five)?$").find(lower)
            ?: Regex("^explain (?:this )?simply$").find(lower)?.let { return "Sure — paste or type what you want explained and I'll break it into plain words, an example, and why it matters." }
            ?: return null
        val topic = m.groupValues[1].trim().replace(Regex("[^a-z0-9 ]"), "").replace("wi fi", "wifi")
        if (topic.isBlank()) return null
        if (topic in setOf("this", "it", "that", "this simply", "it simply")) {
            return "Sure — paste or type what you want explained and I'll break it into plain words, an example, and why it matters."
        }
        if (Regex("\\d").containsMatchIn(topic) && Regex("[+*/x-]").containsMatchIn(topic)) return null
        val hit = GLOSSARY[topic] ?: GLOSSARY.entries.firstOrNull { (k, _) ->
            Regex("\\b${Regex.escape(k)}\\b").containsMatchIn(topic)
        }?.value
        if (hit != null) return "$hit\nWant an example, or how it affects you?"
        return "On \"$topic\" — I don't have that in my offline knowledge, so here's how to break it down:\n" +
            "1. What is it? (one-sentence definition)\n" +
            "2. How does it work? (the main parts)\n" +
            "3. An everyday example.\n" +
            "4. Why it matters to you.\n" +
            "Tell me what you already know and I'll help fill gaps. " +
            "If you save your own cloud on the Online tab, it can answer general-knowledge questions in full."
    }

    // ---------- ideas / options ----------

    private val IDEA_BANKS = mapOf(
        "dinner" to listOf("Stir-fry with whatever veg you have (15 min)", "Pasta with garlic, olive oil, chilli and greens", "Tacos or wraps — quick and flexible", "Sheet-pan chicken or tofu with roast veg", "Fried rice using leftover rice"),
        "lunch" to listOf("Wrap with salad and protein", "Leftovers reheated", "Soup and toast", "Rice bowl with egg and veg"),
        "breakfast" to listOf("Oats with fruit", "Eggs on toast", "Yoghurt, muesli and banana", "Smoothie"),
        "weekend" to listOf("Beach or river walk early before the heat", "Try a new café or market", "Cook something new with a friend", "A day trip — hills, a national park, or the coast", "Reset day: tidy, plan the week, early night"),
        "gift" to listOf("An experience (class, tickets, dinner)", "Something for their hobby", "A personal photo book or note", "Good-quality everyday item they'd never buy themselves"),
        "workout" to listOf("20-min walk + 3 rounds of squats, push-ups, planks", "Bodyweight circuit: 40s on / 20s off × 10", "Bike ride or swim", "Stretch and mobility session"),
        "date" to listOf("Sunset picnic", "Cook together", "Mini golf or bowling", "Walk and dessert somewhere new"),
        "study" to listOf("Active recall: test yourself instead of rereading", "Teach the topic out loud", "Spaced repetition flashcards", "Past papers under timed conditions"),
        "business" to listOf("Solve a problem you personally have", "Offer a local service (cleaning, tutoring, repairs)", "Sell a skill online (design, writing, editing)", "Start tiny: one paying customer before building more"),
        "name" to listOf("Combine two meaningful words", "Use a place or local landmark", "Short, easy to say and spell", "Check the domain and social handles are free")
    )

    private fun ideas(lower: String): String? {
        if (!Regex("\\b(ideas?|options?|suggestions?|suggest|recommend|what should i (?:eat|cook|make|do)|give me \\d+)\\b").containsMatchIn(lower)) return null
        val key = IDEA_BANKS.keys.firstOrNull { lower.contains(it) }
            ?: when {
                Regex("\\b(eat|cook|meal)\\b").containsMatchIn(lower) -> "dinner"
                Regex("\\b(do this weekend|saturday|sunday)\\b").containsMatchIn(lower) -> "weekend"
                Regex("\\b(present|birthday)\\b").containsMatchIn(lower) -> "gift"
                Regex("\\b(exercise|gym|fitness)\\b").containsMatchIn(lower) -> "workout"
                else -> null
            }
        val count = Regex("\\b(\\d)\\b").find(lower)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 5)
        if (key != null) {
            val list = IDEA_BANKS.getValue(key).take(count ?: 5)
            return "Some $key ideas:\n" + list.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n") +
                "\nWant me to narrow these down (budget, time, who's coming)?"
        }
        val topic = topicOf(lower)
        return "Ideas for $topic:\n" +
            "1. The simple option — lowest effort that still works.\n" +
            "2. The classic — what most people do, because it's reliable.\n" +
            "3. The bold one — bigger payoff, more effort.\n" +
            "4. The cheap one — same goal, minimal spend.\n" +
            "5. The fun one — pick what you'd enjoy.\n" +
            "Tell me your budget and time and I'll make these concrete."
    }

    private fun compare(lower: String): String? {
        val m = Regex("^(?:which is better[,:]?\\s+|compare\\s+)?(.+?)\\s+(?:vs\\.?|versus)\\s+(.+?)(?:\\s+which is better)?$").find(lower)
            ?: Regex("^(?:which is better[,:]?|compare)\\s+(.+?)\\s+(?:or|and)\\s+(.+)$").find(lower)
            ?: return null
        val a = m.groupValues[1].replace(Regex("^(which is better|compare)\\s+"), "").trim()
        val b = m.groupValues[2].trim()
        if (a.length > 40 || b.length > 40 || a.isBlank() || b.isBlank()) return null
        return "$a vs $b — compare on:\n" +
            "- Cost (upfront and ongoing)\n- Quality / reliability\n- Ease of use\n- Fit for your situation\n" +
            "Rule of thumb: choose the one that's best on the factor you care about most. Which factor matters most to you?"
    }

    // ---------- drafting ----------

    private fun draft(lower: String, original: String): String? {
        val m = Regex("\\b(?:write|draft|compose|help me write|help me with)\\s+(?:an?\\s+|my\\s+)?(message|text|email|letter|note|reply|apology|excuse|cover letter|caption|bio)\\b(?:\\s+(?:to|for)\\s+(?:my\\s+)?([a-z]+))?(?:.*?\\babout\\s+(.+))?").find(lower)
            ?: return null
        val kind = m.groupValues[1]
        val who = m.groupValues[2].ifBlank { "there" }.replaceFirstChar { it.titlecase(AU) }
        val about = m.groupValues[3].trim().ifBlank { "" }
        val subject = if (about.isNotBlank()) about else "[what it's about]"
        return when (kind) {
            "email", "letter", "cover letter" ->
                "Draft $kind:\n\nSubject: ${subject.replaceFirstChar { it.titlecase(AU) }}\n\nHi $who,\n\n" +
                    "I hope you're well. I'm writing about $subject.\n\n[One or two sentences with the key detail.]\n\n" +
                    "Could you [what you need] by [when]? Happy to chat if that's easier.\n\nThanks,\n[Your name]\n\n" +
                    "Tell me the details and I'll tighten it."
            "apology" ->
                "Draft apology:\n\nHi $who, I'm sorry about $subject. That wasn't fair to you, and I get why you're upset. " +
                    "Next time I'll [what you'll do differently]. Can we talk when you're free?"
            "caption" -> "Caption ideas for $subject:\n1. Good times, better people.\n2. Saving this one.\n3. $subject, but make it memorable."
            "bio" -> "Short bio:\n[Name] — [what you do] who loves [interest]. Based in Perth. [One fun fact]."
            else ->
                "Draft $kind:\n\nHey $who, just a quick one about $subject — [key detail]. Let me know what you think 🙂\n\n" +
                    "Want it more formal, shorter, or friendlier?"
        }
    }

    // ---------- helpers ----------

    private val STOP = setOf(
        "the", "and", "for", "you", "are", "was", "not", "but", "can", "how", "why", "who", "what",
        "give", "some", "ideas", "idea", "options", "option", "suggest", "suggestions", "please", "about",
        "with", "that", "this", "need", "want", "help", "make", "get", "into", "should", "would", "could"
    )

    fun topicOf(text: String): String {
        val words = text.lowercase(AU).split(Regex("[^a-z0-9]+")).filter { it.length >= 3 && it !in STOP }
        return words.take(3).joinToString(" ").ifBlank { "that" }
    }

    private fun fmt(n: Double): String = LocalTools.formatAmount(n)
}
