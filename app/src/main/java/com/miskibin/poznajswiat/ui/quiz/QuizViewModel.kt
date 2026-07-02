package com.miskibin.poznajswiat.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.Country
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

/**
 * One quiz question. [kind] decides the rendering: country-based kinds carry
 * [target] (+[options] for flags/capitals), HISTORY carries [event]+[yearOptions].
 */
data class Question(
    val kind: QuizMode,
    val target: Country? = null,
    val options: List<Country> = emptyList(),
    val event: HistoryEvent? = null,
    val yearOptions: List<Int> = emptyList(),
    /** FLAGS only: show the country name, choose among four flags. */
    val reverse: Boolean = false,
) {
    val itemId: String
        get() = itemId(kind, target?.cca2 ?: event!!.id.toString())

    /** The option key that is the correct answer (cca2 or year as string). */
    val correctKey: String
        get() = target?.cca2 ?: event!!.year.toString()
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
) {
    val current: Question? get() = questions.getOrNull(index)
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
            val questions = when (session) {
                SESSION_REVIEW -> buildReviewSession(data, progress)
                SESSION_MIX -> buildMixSession(data, progress)
                QuizMode.HISTORY.id -> buildHistorySession(data, progress)
                else -> buildCountrySession(data, progress, QuizMode.fromId(session))
            }
            _state.value = QuizUiState(loading = false, data = data, questions = questions)
        }
    }

    /** Weighted pick without replacement: lower streak -> more likely. */
    private fun <T> pickWeighted(
        pool: List<T>,
        count: Int,
        streakOf: (T) -> Int,
    ): List<T> {
        val remaining = pool.toMutableList()
        val picked = mutableListOf<T>()
        repeat(min(count, remaining.size)) {
            val weights = remaining.map {
                (MASTERY_STREAK + 1) - min(streakOf(it), MASTERY_STREAK)
            }
            var roll = Random.nextInt(weights.sum())
            var idx = 0
            while (roll >= weights[idx]) {
                roll -= weights[idx]; idx++
            }
            picked += remaining.removeAt(idx)
        }
        return picked
    }

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

    private fun historyQuestion(event: HistoryEvent): Question {
        // Year distractors scale with how long ago the event happened, so
        // ancient events get century-scale offsets and modern ones small gaps.
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
            progress.streak(mode, it.cca2)
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
            progress.streak(mode, c.cca2)
        }.map { (mode, c) -> countryQuestion(data, mode, c) }

        val relatedEvents = data.events.filter { e -> e.countries.any { it in poolCodes } }
        val historyPart = pickWeighted(relatedEvents, 2) {
            progress.streak(QuizMode.HISTORY, it.id.toString())
        }.map { historyQuestion(it) }

        return (countryPart + historyPart).shuffled()
    }

    private fun buildHistorySession(data: AppData, progress: Progress): List<Question> {
        val targets = pickWeighted(data.events, QUESTIONS_PER_SESSION) {
            progress.streak(QuizMode.HISTORY, it.id.toString())
        }
        return targets.map { historyQuestion(it) }
    }

    private fun buildReviewSession(data: AppData, progress: Progress): List<Question> {
        val today = LocalDate.now().toEpochDay()
        val due = progress.dueItems(today).shuffled().take(REVIEW_SESSION_SIZE)
        val questions = due.mapNotNull { id ->
            val mode = QuizMode.entries.firstOrNull { id.startsWith("${it.id}/") }
                ?: return@mapNotNull null
            val key = id.substringAfter('/')
            when (mode) {
                QuizMode.HISTORY -> data.eventsById[key.toIntOrNull()]?.let { historyQuestion(it) }
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
                if (mode == QuizMode.HISTORY) historyQuestion(item as HistoryEvent)
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
