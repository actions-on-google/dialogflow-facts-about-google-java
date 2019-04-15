/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.google.actions.api.ActionContext;
import com.google.actions.api.ActionRequest;
import com.google.actions.api.ActionResponse;
import com.google.actions.api.DialogflowApp;
import com.google.actions.api.ForIntent;
import com.google.actions.api.response.ResponseBuilder;
import com.google.api.services.actions_fulfillment.v2.model.BasicCard;
import com.google.api.services.actions_fulfillment.v2.model.Button;
import com.google.api.services.actions_fulfillment.v2.model.Image;
import com.google.api.services.actions_fulfillment.v2.model.OpenUrlAction;
import com.google.api.services.actions_fulfillment.v2.model.SimpleResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

public class FactsAboutGoogle extends DialogflowApp {

  // Suggestion chip constants
  private static final String[] CONFIRMATION_SUGGESTIONS = new String[]{"Sure", "No thanks"};
  private static final String[] INTENT_SUGGESTIONS = new String[]{"History", "Headquarters"};
  private static final HashMap<String, String[]> SINGLE_CATEGORY_SUGGESTIONS;
  // Fact constants
  private static final List<String> INITIAL_CAT_FACTS =
      Arrays.asList("cat_fact_1", "cat_fact_2", "cat_fact_3");
  private static final List<String> INITIAL_HISTORY_FACTS =
      Arrays.asList(
          "google_history_fact_1",
          "google_history_fact_2",
          "google_history_fact_3",
          "google_history_fact_4");
  private static final List<String> INITIAL_HEADQUARTERS_FACTS =
      Arrays.asList(
          "google_headquarters_fact_1", "google_headquarters_fact_2",
          "google_headquarters_fact_3");
  // Card constants
  private static final HashMap<String, String> GOOGLE_CARD;

  static {
    GOOGLE_CARD = new HashMap<>();
    GOOGLE_CARD.put("url", "google_app_logo_url");
    GOOGLE_CARD.put("a11y", "google_app_logo_a11y");
  }

  private static final HashMap<String, String> STAN_CARD;

  static {
    STAN_CARD = new HashMap<>();
    STAN_CARD.put("url", "stan_url");
    STAN_CARD.put("a11y", "stan_a11y");
  }

  private static final HashMap<String, String> GOOGLEPLEX_CARD;

  static {
    GOOGLEPLEX_CARD = new HashMap<>();
    GOOGLEPLEX_CARD.put("url", "googleplex_url");
    GOOGLEPLEX_CARD.put("a11y", "googleplex_a11y");
  }

  private static final HashMap<String, String> GOOGLEPLEX_BIKE_CARD;

  static {
    GOOGLEPLEX_BIKE_CARD = new HashMap<>();
    GOOGLEPLEX_BIKE_CARD.put("url", "googleplex_biking_url");
    GOOGLEPLEX_BIKE_CARD.put("a11y", "googleplex_biking_a11y");
  }

  private static final List<Map<String, String>> CARDS =
      Arrays.asList(GOOGLE_CARD, STAN_CARD, GOOGLEPLEX_CARD,
          GOOGLEPLEX_BIKE_CARD);

  static {
    SINGLE_CATEGORY_SUGGESTIONS = new HashMap<>();
    SINGLE_CATEGORY_SUGGESTIONS
        .put("headquarters", new String[]{"Headquarters"});
    SINGLE_CATEGORY_SUGGESTIONS.put("history", new String[]{"History"});
  }

  @ForIntent("Unrecognized Deep Link")
  public ActionResponse deepLinkWelcome(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources");
    responseBuilder
        .add(
            String.format(
                rb.getString("deep_link_fallback"),
                ((String) request.getParameter("any"))))
        .addSuggestions(getCategorySuggestionsList());

    return responseBuilder.build();
  }

  // Fulfill "choose_fact" and "tell_fact" intent fact fulfillment function
  @ForIntent("choose_fact")
  public ActionResponse chooseFact(ActionRequest request) {
    return fact(request);
  }

  @ForIntent("tell_fact")
  public ActionResponse tellFact(ActionRequest request) {
    return fact(request);
  }

  private ActionResponse fact(ActionRequest request) {
    String selectedCategory = ((String) request.getParameter("category"));
    Map<String, Object> conversationData = request.getConversationData();

    // Set the initial facts
    if (conversationData.get("history") == null
        && conversationData.get("headquarters") == null
        && conversationData.get("cats") == null) {
      conversationData.put("history", INITIAL_HISTORY_FACTS);
      conversationData.put("headquarters", INITIAL_HEADQUARTERS_FACTS);
      conversationData.put("cats", INITIAL_CAT_FACTS);
    }

    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources");
    List<String> facts = (List<String>) conversationData.get(selectedCategory);

    List<String> historyFacts = (List<String>) conversationData.get("history");
    List<String> headquartersFacts = (List<String>) conversationData
        .get("headquarters");
    List<String> catFacts = (List<String>) conversationData.get("cats");

    if (historyFacts.isEmpty() && headquartersFacts.isEmpty()) {
      // no facts are left
      responseBuilder.add(rb.getString("heardItAll")).endConversation();
    } else if (facts.isEmpty()) {
      // Suggest other category if no more facts in current category
      String otherCategory = ((selectedCategory == "history") ? "history"
          : "headquarters");
      String response =
          String.format(rb.getString("factTransition"), selectedCategory,
              otherCategory);
      List<String> suggestions = new ArrayList<>();
      Collections
          .addAll(suggestions, SINGLE_CATEGORY_SUGGESTIONS.get(otherCategory));
      // Mention and suggest cats if there are cat facts left
      if (!catFacts.isEmpty()) {
        response = String
            .format(rb.getString("factTransitionToCats"), response);
        suggestions.add("Cats");
      }

      // Add context to redirect to other fact category
      Map<String, String> contextParameter = new HashMap<>();
      contextParameter.put("category", otherCategory);
      ActionContext context = new ActionContext("choose_fact-followup", 5);
      context.setParameters(contextParameter);

      responseBuilder.add(response)
          .addSuggestions(suggestions.toArray(new String[0])).add(context);
    } else {
      // There are facts remaining in the currently selected category
      Random random = new Random();
      int factIndex = random.nextInt(facts.size());
      String fact = facts.get(factIndex);
      // Update user storage to remove fact that will be said to user
      List<String> updatedFacts = new ArrayList<>(facts);
      updatedFacts.remove(factIndex);
      conversationData.put(selectedCategory, updatedFacts);

      // Get random card image and accessibility text
      int cardIndex = random.nextInt(CARDS.size());
      String imageUrl = CARDS.get(cardIndex).get("url");
      String imageA11y = CARDS.get(cardIndex).get("a11y");
      // Setup button for card
      Button learnMoreButton =
          new Button()
              .setTitle(rb.getString("card_link_out_text"))
              .setOpenUrlAction(new OpenUrlAction()
                  .setUrl(rb.getString("card_link_out_url")));

      responseBuilder
          .add(
              String.format(rb.getString(selectedCategory), rb.getString(fact)))
          .add(rb.getString("nextFact"))
          .add(
              new BasicCard()
                  .setTitle(rb.getString(fact))
                  .setImage(
                      new Image()
                          .setUrl(rb.getString(imageUrl))
                          .setAccessibilityText(rb.getString(imageA11y)))
                  .setButtons(Collections.singletonList(learnMoreButton)))
          .addSuggestions(CONFIRMATION_SUGGESTIONS);
    }
    return responseBuilder.build();
  }

  // Fulfill "choose_cats" and "tell_cat_fact" intent fact fulfillment function
  @ForIntent("choose_cats")
  public ActionResponse chooseCats(ActionRequest request) {
    return catFact(request);
  }

  @ForIntent("tell_cat_fact")
  public ActionResponse tellCatFact(ActionRequest request) {
    return catFact(request);
  }

  private ActionResponse catFact(ActionRequest request) {
    Map<String, Object> conversationData = request.getConversationData();

    // Set the initial facts
    if (conversationData.get("history") == null
        && conversationData.get("headquarters") == null
        && conversationData.get("cats") == null) {
      conversationData.put("history", INITIAL_HISTORY_FACTS);
      conversationData.put("headquarters", INITIAL_HEADQUARTERS_FACTS);
      conversationData.put("cats", INITIAL_CAT_FACTS);
    }
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources");
    List<String> facts = ((List<String>) conversationData.get("cats"));
    if (facts.isEmpty()) {
      responseBuilder
          .add(rb.getString("factTransitionFromCats"))
          .addSuggestions(INTENT_SUGGESTIONS)
          .removeContext("choose_fact-followup")
          .removeContext("choose_cats-followup");
    } else {
      // Get random cat fact
      Random random = new Random();
      int factIndex = random.nextInt(facts.size());
      String fact = facts.get(factIndex);
      // Update user storage to remove cat fact that will be said to user
      List<String> updatedCatFacts = new ArrayList<>(facts);
      updatedCatFacts.remove(factIndex);
      conversationData.put("cats", updatedCatFacts);

      // Construct cat fact card
      String imageUrl = rb.getString("cat_img_url");
      String imageA11y = rb.getString("cat_img_a11y");
      // Setup button for card
      Button learnMoreButton =
          new Button()
              .setTitle(rb.getString("card_link_out_text"))
              .setOpenUrlAction(
                  new OpenUrlAction().setUrl(rb.getString("cat_url")));
      List<Button> buttons = new ArrayList<>();
      buttons.add(learnMoreButton);

      // build response
      responseBuilder
          .add(
              new SimpleResponse()
                  .setDisplayText(rb.getString("cat_prefix"))
                  .setTextToSpeech(
                      String.format(
                          rb.getString("cat_ssml"),
                          rb.getString("cat_prefix"),
                          rb.getString(fact))))
          .add(rb.getString("nextFact"))
          .add(
              new BasicCard()
                  .setTitle(rb.getString(fact))
                  .setImage(new Image().setUrl(imageUrl)
                      .setAccessibilityText(imageA11y))
                  .setButtons(buttons))
          .addSuggestions(CONFIRMATION_SUGGESTIONS);
    }
    return responseBuilder.build();
  }

  private String[] getCategorySuggestionsList() {
    List<String> categorySuggestionsList = new ArrayList<String>();
    for (String[] categorySuggestions : SINGLE_CATEGORY_SUGGESTIONS.values()) {
      for (String suggestion : categorySuggestions) {
        categorySuggestionsList.add(suggestion);
      }
    }
    return categorySuggestionsList
        .toArray(new String[categorySuggestionsList.size()]);
  }
}
