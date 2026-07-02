package com.miskibin.poznajswiat.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.Country
import com.miskibin.poznajswiat.data.MASTERY_STREAK
import com.miskibin.poznajswiat.data.ProgressRepo
import com.miskibin.poznajswiat.data.QUESTIONS_PER_SESSION
import com.miskibin.poznajswiat.data.QuizMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

data class Question(
    val target: Country,
    val options: List<Country>,
)

data class QuestionResult(
    val country: Country,
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
    val mode: QuizMode,
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
            val pool = data.forContinent(continent)
            val count = min(QUESTIONS_PER_SESSION, pool.size)

            // Weighted pick without replacement: countries with a lower streak
            // (less mastered) are more likely to appear.
            val remaining = pool.toMutableList()
            val targets = mutableListOf<Country>()
            repeat(count) {
                val weights = remaining.map {
                    (MASTERY_STREAK + 1) - min(progress.streak(mode, it.cca2), MASTERY_STREAK)
                }
                var roll = Random.nextInt(weights.sum())
                var idx = 0
                while (roll >= weights[idx]) {
                    roll -= weights[idx]; idx++
                }
                targets += remaining.removeAt(idx)
            }

            val questions = targets.map { target ->
                val sameContinent = data.countries
                    .filter { it.continent == target.continent && it.cca2 != target.cca2 }
                    .shuffled()
                val others = data.countries
                    .filter { it.continent != target.continent }
                    .shuffled()
                val distractors = (sameContinent + others).take(3)
                Question(target, (distractors + target).shuffled())
            }
            _state.value = QuizUiState(loading = false, data = data, questions = questions)
        }
    }

    /** Answer for option-based modes (flags, capitals). */
    fun answer(cca2: String) {
        val s = _state.value
        val q = s.current ?: return
        if (s.answered) return
        val correct = cca2 == q.target.cca2
        _state.value = s.copy(
            answered = true,
            selected = cca2,
            score = s.score + if (correct) 1 else 0,
            results = s.results + QuestionResult(q.target, correct),
        )
        viewModelScope.launch { repo.record(mode, q.target.cca2, correct) }
    }

    /** Tap on the map (map mode). [cca2] is null when the tap hit open water. */
    fun mapTap(cca2: String?) {
        val s = _state.value
        val q = s.current ?: return
        if (s.answered) return
        if (cca2 == q.target.cca2) {
            val firstTry = s.triesLeft == 3
            _state.value = s.copy(
                answered = true,
                score = s.score + if (firstTry) 1 else 0,
                lastWrongTap = null,
                results = s.results + QuestionResult(q.target, firstTry),
            )
            viewModelScope.launch { repo.record(mode, q.target.cca2, firstTry) }
        } else {
            val tries = s.triesLeft - 1
            if (tries <= 0) {
                _state.value = s.copy(
                    answered = true,
                    revealed = true,
                    triesLeft = 0,
                    lastWrongTap = cca2,
                    wrongTaps = if (cca2 != null) s.wrongTaps + cca2 else s.wrongTaps,
                    results = s.results + QuestionResult(q.target, false),
                )
                viewModelScope.launch { repo.record(mode, q.target.cca2, false) }
            } else {
                _state.value = s.copy(
                    triesLeft = tries,
                    lastWrongTap = cca2,
                    wrongTaps = if (cca2 != null) s.wrongTaps + cca2 else s.wrongTaps,
                )
            }
        }
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
        private val mode: QuizMode,
        private val continent: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuizViewModel(app, mode, continent) as T
    }
}
