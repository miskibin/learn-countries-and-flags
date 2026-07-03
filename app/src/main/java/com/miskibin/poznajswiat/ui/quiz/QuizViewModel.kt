package com.miskibin.poznajswiat.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.Country
import com.miskibin.poznajswiat.data.EVENT_THEMES
import com.miskibin.poznajswiat.data.HistoryEvent
import com.miskibin.poznajswiat.data.MASTERY_STREAK
import com.miskibin.poznajswiat.data.Progress
import com.miskibin.poznajswiat.data.ProgressRepo
import com.miskibin.poznajswiat.data.QUESTIONS_PER_SESSION
import com.miskibin.poznajswiat.data.QuizMode
import com.miskibin.poznajswiat.data.REVIEW_SESSION_SIZE
import com.miskibin.poznajswiat.data.SESSION_MIX
import com.miskibin.poznajswiat.data.SESSION_REVIEW
import com.miskibin.poznajswiat.data.itemId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** The three shapes a history question can take. */
enum class HistoryForm { YEAR, ORDER, COUNTRY }

const val SESSION_BONUS_XP = 5
const val PERFECT_BONUS_XP = 20

/**
 * One quiz question. [kind] decides the rendering: country-based kinds carry
 * [target] (+[options] for flags/capitals); HISTORY carries [event] and,
 * depending on [form], year options, a second event to order against, or
 * country options (with [target] as the right country).
 */
data class Question(
    val kind: QuizMode,
    val target: Country? = null,
    val options: List<Country> = emptyList(),
    val event: HistoryEvent? = null,
    val yearOptions: List<Int> = emptyList(),
    /** FLAGS only: show the country name, choose among four flags. */
    val reverse: Boolean = false,
    val form: HistoryForm = HistoryForm.YEAR,
    val eventB: HistoryEvent? = null,
) {
    val itemId: String
        get() = if (kind == QuizMode.HISTORY) itemId(kind, event!!.id.toString())
        else itemId(kind, target!!.cca2)

    /** The option key that is the correct answer. */
    val correctKey: String
        get() = when {
            kind != QuizMode.HISTORY -> target!!.cca2
            form == HistoryForm.YEAR -> event!!.year.toString()
            form == HistoryForm.ORDER -> {
                val earlier = if (event!!.year <= eventB!!.year) event else eventB
                earlier.id.toString()
            }
            else -> target!!.cca2 // COUNTRY
        }
}

data class QuestionResult(
    val question: Question,
    val correct: Boolean,
)

data class QuizUiState(
    val loading: Boolean = true,
    val data: AppData? = null,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val score: Int = 0,
    val answered: Boolean = false,
    val selected: String? = null,
    val wrongTaps: Set<String> = emptySet(),
    val triesLeft: Int = 3,
    val revealed: Boolean = false,
    val lastWrongTap: String? = null,
    val results: List<QuestionResult> = emptyList(),
    val finished: Boolean = false,
    /** Consecutive correct answers within this session. */
    val combo: Int = 0,
    /** Session focus shown under the progress bar, e.g. "Motyw: Wojny". */
    val sessionLabel: String? = null,
) {
    val current: Question? get() = questions.getOrNull(index)

    val lastCorrect: Boolean get() = answered && results.lastOrNull()?.correct == true
}

class QuizViewModel(
    app: Application,
    val session: String,
    private val continent: String?,
) : AndroidViewModel(app) {

    private val repo = ProgressRepo.get(app)
    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    init {
        startSession()
    }

    fun startSession() {
        _state.value = QuizUiState()
        viewModelScope.launch {
            val data = AppData.get(getApplication())
            val progress = repo.progress.first()
            var label: String? = null
            val questions = when (session) {
                SESSION_REVIEW -> buildReviewSession(data, progress)
                SESSION_MIX -> buildMixSession(data, progress)
                QuizMode.HISTORY.id -> {
                    val (theme, qs) = buildHistorySession(data, progress)
                    label = theme?.let { "Motyw: $it" }
                    qs
                }
                else -> buildCountrySession(data, progress, QuizMode.fromId(session))
            }
            _state.value = QuizUiState(
                loading = false,
                data = data,
                questions = questions,
                sessionLabel = label,
            )
        }
    }

    /**
     * Bigger countries first: weight multiplier by world rank of land area,
     * so learners build the map from its anchors down to microstates.
     */
    private fun areaTier(data: AppData, country: Country): Int {
        val rank = data.areaRank[country.cca2] ?: Int.MAX_VALUE
        return when {
            rank < 60 -> 3
            rank < 130 -> 2
            else -> 1
        }
    }

    /** Weighted pick without replacement. */
    private fun <T> pickWeighted(
        pool: List<T>,
        count: Int,
        weightOf: (T) -> Int,
    ): List<T> {
        val remaining = pool.toMutableList()
        val picked = mutableListOf<T>()
        repeat(min(count, remaining.size)) {
            val weights = remaining.map { weightOf(it).coerceAtLeast(1) }
            var roll = Random.nextInt(weights.sum())
            var idx = 0
            while (roll >= weights[idx]) {
                roll -= weights[idx]; idx++
            }
            picked += remaining.removeAt(idx)
        }
        return picked
    }

    /** Lower streak -> more likely (1..4). */
    private fun streakWeight(streak: Int): Int =
        (MASTERY_STREAK + 1) - min(streak, MASTERY_STREAK)

    private fun countryQuestion(data: AppData, mode: QuizMode, target: Country): Question {
        if (mode == QuizMode.MAP) return Question(kind = mode, target = target)
        val sameContinent = data.countries
            .filter { it.continent == target.continent && it.cca2 != target.cca2 }
            .shuffled()
        val others = data.countries
            .filter { it.continent != target.continent }
            .shuffled()
        val distractors = (sameContinent + others).take(3)
        return Question(
            kind = mode,
            target = target,
            options = (distractors + target).shuffled(),
            reverse = mode == QuizMode.FLAGS && Random.nextFloat() < 0.35f,
        )
    }

    /** Countries a map question can meaningfully ask about. */
    private fun mapPool(countries: List<Country>) = countries.filter { it.hasPoly }

    /** Randomly picks one of three history-question shapes for variety. */
    private fun historyQuestion(data: AppData, event: HistoryEvent): Question {
        val roll = Random.nextFloat()

        // "Which country is this event tied to?"
        val countryTarget = event.countries.firstNotNullOfOrNull { data.byCode[it] }
        if (roll < 0.28f && countryTarget != null) {
            val sameContinent = data.countries
                .filter { it.continent == countryTarget.continent && it.cca2 !in event.countries }
                .shuffled()
            val others = data.countries
                .filter { it.continent != countryTarget.continent && it.cca2 !in event.countries }
                .shuffled()
            return Question(
                kind = QuizMode.HISTORY,
                target = countryTarget,
                options = ((sameContinent + others).take(3) + countryTarget).shuffled(),
                event = event,
                form = HistoryForm.COUNTRY,
            )
        }

        // "Which happened earlier?" — partner must not be a near-tie.
        if (roll < 0.56f) {
            val partner = data.events
                .filter { it.id != event.id && abs(it.year - event.year) >= 25 }
                .randomOrNull()
            if (partner != null) {
                return Question(
                    kind = QuizMode.HISTORY,
                    event = event,
                    eventB = partner,
                    form = HistoryForm.ORDER,
                )
            }
        }

        // "In which year?" — distractors scale with how long ago it happened.
        val spread = max(4, abs(LocalDate.now().year - event.year) / 8)
        val years = mutableSetOf(event.year)
        while (years.size < 4) {
            val offset = Random.nextInt(spread / 3 + 1, spread + 1) *
                (if (Random.nextBoolean()) 1 else -1)
            val y = event.year + offset
            if (y <= LocalDate.now().year) years += y
        }
        return Question(
            kind = QuizMode.HISTORY,
            event = event,
            yearOptions = years.shuffled(),
        )
    }

    private fun buildCountrySession(
        data: AppData,
        progress: Progress,
        mode: QuizMode,
    ): List<Question> {
        val pool = data.forContinent(continent)
            .let { if (mode == QuizMode.MAP) mapPool(it) else it }
        val targets = pickWeighted(pool, QUESTIONS_PER_SESSION) {
            streakWeight(progress.streak(mode, it.cca2)) * areaTier(data, it)
        }
        return targets.map { countryQuestion(data, mode, it) }
    }

    /**
     * Interleaved continent session: flags, map and capitals about the same
     * region mixed together, plus a couple of history events tied to it —
     * switching retrieval modes strengthens retention more than blocks of one
     * question type.
     */
    private fun buildMixSession(data: AppData, progress: Progress): List<Question> {
        val pool = data.forContinent(continent)
        val poolCodes = pool.map { it.cca2 }.toSet()
        val pairs: List<Pair<QuizMode, Country>> = pool.flatMap { c ->
            buildList {
                add(QuizMode.FLAGS to c)
                add(QuizMode.CAPITALS to c)
                if (c.hasPoly) add(QuizMode.MAP to c)
            }
        }
        val countryPart = pickWeighted(pairs, QUESTIONS_PER_SESSION) { (mode, c) ->
            streakWeight(progress.streak(mode, c.cca2)) * areaTier(data, c)
        }.map { (mode, c) -> countryQuestion(data, mode, c) }

        val relatedEvents = data.events.filter { e -> e.countries.any { it in poolCodes } }
        val historyPart = pickWeighted(relatedEvents, 2) {
            streakWeight(progress.streak(QuizMode.HISTORY, it.id.toString()))
        }.map { historyQuestion(data, it) }

        return (countryPart + historyPart).shuffled()
    }

    /**
     * One THEME per history session, questions in chronological order —
     * related events studied together form a coherent narrative instead of
     * random jumps across millennia.
     */
    private fun buildHistorySession(data: AppData, progress: Progress): Pair<String?, List<Question>> {
        val byTheme = EVENT_THEMES.mapNotNull { theme ->
            val events = data.events.filter { theme in it.tags }
            if (events.size < 4) null
            else theme to events
        }
        if (byTheme.isEmpty()) {
            val targets = pickWeighted(data.events, QUESTIONS_PER_SESSION) {
                streakWeight(progress.streak(QuizMode.HISTORY, it.id.toString()))
            }
            return null to targets.sortedBy { it.year }.map { historyQuestion(data, it) }
        }
        // Prefer themes with the most unmastered material.
        val theme = pickWeighted(byTheme, 1) { (_, events) ->
            events.count { !progress.isMastered(QuizMode.HISTORY, it.id.toString()) }
        }.first()
        val targets = pickWeighted(theme.second, QUESTIONS_PER_SESSION) {
            streakWeight(progress.streak(QuizMode.HISTORY, it.id.toString()))
        }
        return theme.first to targets.sortedBy { it.year }.map { historyQuestion(data, it) }
    }

    private fun buildReviewSession(data: AppData, progress: Progress): List<Question> {
        val today = LocalDate.now().toEpochDay()
        val due = progress.dueItems(today).shuffled().take(REVIEW_SESSION_SIZE)
        val questions = due.mapNotNull { id ->
            val mode = QuizMode.entries.firstOrNull { id.startsWith("${it.id}/") }
                ?: return@mapNotNull null
            val key = id.substringAfter('/')
            when (mode) {
                QuizMode.HISTORY -> data.eventsById[key.toIntOrNull()]?.let { historyQuestion(data, it) }
                QuizMode.MAP -> data.byCode[key]?.takeIf { it.hasPoly }
                    ?.let { countryQuestion(data, mode, it) }
                else -> data.byCode[key]?.let { countryQuestion(data, mode, it) }
            }
        }.toMutableList()

        // Top up with unseen material so the daily session is always worthwhile.
        if (questions.size < QUESTIONS_PER_SESSION) {
            val have = questions.map { it.itemId }.toSet()
            val candidates: List<Pair<QuizMode, Any>> =
                data.countries.flatMap { c ->
                    buildList {
                        add(QuizMode.FLAGS to c)
                        add(QuizMode.CAPITALS to c)
                        if (c.hasPoly) add(QuizMode.MAP to c)
                    }
                } + data.events.map { QuizMode.HISTORY to it }
            val fresh = candidates.filter { (mode, item) ->
                val key = (item as? Country)?.cca2 ?: (item as HistoryEvent).id.toString()
                val id = itemId(mode, key)
                id !in have && (progress.attempts[id] ?: 0) == 0
            }.shuffled().take(QUESTIONS_PER_SESSION - questions.size)
            questions += fresh.map { (mode, item) ->
                if (mode == QuizMode.HISTORY) historyQuestion(data, item as HistoryEvent)
                else countryQuestion(data, mode, item as Country)
            }
        }
        return questions.shuffled()
    }

    /** Answer for option-based questions (flags, capitals, history years). */
    fun answer(key: String) {
        val s = _state.value
        val q = s.current ?: return
        if (s.answered) return
        val correct = key == q.correctKey
        _state.value = s.copy(
            answered = true,
            selected = key,
            score = s.score + if (correct) 1 else 0,
            combo = if (correct) s.combo + 1 else 0,
            results = s.results + QuestionResult(q, correct),
        )
        viewModelScope.launch { repo.record(q.itemId, correct) }
    }

    /** Tap on the map (map questions). [cca2] is null when the tap hit water. */
    fun mapTap(cca2: String?) {
        val s = _state.value
        val q = s.current ?: return
        if (s.answered || q.kind != QuizMode.MAP) return
        if (cca2 == q.target?.cca2) {
            val firstTry = s.triesLeft == 3
            _state.value = s.copy(
                answered = true,
                score = s.score + if (firstTry) 1 else 0,
                combo = if (firstTry) s.combo + 1 else 0,
                lastWrongTap = null,
                results = s.results + QuestionResult(q, firstTry),
            )
            viewModelScope.launch { repo.record(q.itemId, firstTry) }
        } else {
            val tries = s.triesLeft - 1
            if (tries <= 0) {
                _state.value = s.copy(
                    answered = true,
                    revealed = true,
                    triesLeft = 0,
                    combo = 0,
                    lastWrongTap = cca2,
                    wrongTaps = if (cca2 != null) s.wrongTaps + cca2 else s.wrongTaps,
                    results = s.results + QuestionResult(q, false),
                )
                viewModelScope.launch { repo.record(q.itemId, false) }
            } else {
                _state.value = s.copy(
                    triesLeft = tries,
                    lastWrongTap = cca2,
                    wrongTaps = if (cca2 != null) s.wrongTaps + cca2 else s.wrongTaps,
                )
            }
        }
    }

    /** "I don't know" on a map question: reveal the country, counts as wrong. */
    fun reveal() {
        val s = _state.value
        val q = s.current ?: return
        if (s.answered || q.kind != QuizMode.MAP) return
        _state.value = s.copy(
            answered = true,
            revealed = true,
            triesLeft = 0,
            combo = 0,
            lastWrongTap = null,
            results = s.results + QuestionResult(q, false),
        )
        viewModelScope.launch { repo.record(q.itemId, false) }
    }

    fun next() {
        val s = _state.value
        if (!s.answered) return
        if (s.index + 1 >= s.questions.size) {
            _state.value = s.copy(finished = true)
            // Session-completion bonus, doubled up for a perfect run.
            val bonus = SESSION_BONUS_XP +
                if (s.score == s.questions.size && s.questions.isNotEmpty()) PERFECT_BONUS_XP else 0
            viewModelScope.launch { repo.addXp(bonus) }
        } else {
            _state.value = s.copy(
                index = s.index + 1,
                answered = false,
                selected = null,
                wrongTaps = emptySet(),
                triesLeft = 3,
                revealed = false,
                lastWrongTap = null,
            )
        }
    }

    class Factory(
        private val app: Application,
        private val session: String,
        private val continent: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuizViewModel(app, session, continent) as T
    }
}
