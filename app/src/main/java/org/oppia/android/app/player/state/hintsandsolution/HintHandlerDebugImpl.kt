package org.oppia.android.app.player.state.hintsandsolution

import org.oppia.android.app.devoptions.ShowAllHintsAndSolutionChecker
import org.oppia.android.app.model.PendingState
import org.oppia.android.app.model.State
import org.oppia.android.domain.exploration.ExplorationProgressController
import javax.inject.Inject

/**
 * [HintHandler] for showing all hints and solution based on availability, bypassing the wrong
 * answer check and scheduling of hints, if 'Show all hints and solution' functionality is enabled
 * in the 'Developer Options Menu'. If this functionality is disabled then it will fall back to the
 * [HintHandlerProdImpl] and provide its functionalities.
 */
class HintHandlerDebugImpl @Inject constructor(
  private val hintHandlerProdImpl: HintHandlerProdImpl,
  private val explorationProgressController: ExplorationProgressController,
  private val showAllHintsAndSolutionChecker: ShowAllHintsAndSolutionChecker
) : HintHandler {

  override fun reset() {
    hintHandlerProdImpl.reset()
  }

  override fun hideHint() {
    hintHandlerProdImpl.hideHint()
  }

  override fun maybeScheduleShowHint(state: State, pendingState: PendingState) {
    if (showAllHintsAndSolutionChecker.getShowAllHintsAndSolution()) showAllHintsAndSolution(state)
    else hintHandlerProdImpl.maybeScheduleShowHint(state, pendingState)
  }

  /** Shows all hints and solution. */
  private fun showAllHintsAndSolution(state: State) {
    if (state.interaction.hintList.isEmpty()) {
      // If this state has no hints to show, do nothing.
      return
    }

    hintHandlerProdImpl.checkForHintsToBeRevealed(state)

    state.interaction.hintList.forEach { _ ->
      val nextUnrevealedHintIndex = hintHandlerProdImpl.getNextHintIndexToReveal(state)
      hintHandlerProdImpl.showHintImmediately(nextUnrevealedHintIndex)
      explorationProgressController.submitHintIsRevealed(
        state,
        true,
        nextUnrevealedHintIndex.hintIndex
      )
    }
    if (state.interaction.hintList.last().hintIsRevealed && state.interaction.hasSolution()) {
      explorationProgressController.submitSolutionIsRevealed(state)
    }
  }
}