package com.larkspur.customers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@CommerceServiceTest
class CustomerAccessTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void signupSigninAndMe() throws Exception {
    String email = "ada-" + UUID.randomUUID() + "@example.test";
    MvcResult signup = mvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.signup("Ada Lovelace", email, "password12"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customer.email").value(email))
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andReturn();
    String token = json.readTree(signup.getResponse().getContentAsString()).get("token").asText();

    mvc.perform(post("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"password12\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customer.email").value(email));

    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email));
  }

  @Test
  void wrongPasswordIs401() throws Exception {
    String email = "wrong-" + UUID.randomUUID() + "@example.test";
    signup(email);
    mvc.perform(post("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"not-the-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Email or password is incorrect."));
  }

  @Test
  void suspendedAccountIs403() throws Exception {
    String email = "suspended-" + UUID.randomUUID() + "@example.test";
    mvc.perform(post("/api/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.customer("Grace Hopper", email, "password12", "SUSPENDED"))))
        .andExpect(status().isCreated());
    mvc.perform(post("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"password12\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("This account is suspended."));
  }

  @Test
  void missingAndInvalidTokenAre401() throws Exception {
    mvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to continue."));
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to continue."));
  }

  @Test
  void unknownCustomerIs404() throws Exception {
    mvc.perform(get("/api/customers/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Customer not found."));
  }

  @Test
  void duplicateEmailIs409() throws Exception {
    String email = "dup-" + UUID.randomUUID() + "@example.test";
    signup(email);
    mvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.signup("Second Ada", email, "password12"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("An account with this email already exists."));
  }

  @Test
  void shortSignupPasswordIs400() throws Exception {
    mvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.signup("Ada Lovelace", "short-" + UUID.randomUUID() + "@example.test", "short"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("password size must be between 8 and 80"));
  }

  @Test
  void shortCustomerPasswordIs400() throws Exception {
    mvc.perform(post("/api/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.customer("Ada Lovelace", "desk-" + UUID.randomUUID() + "@example.test", "short", "ACTIVE"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("password must be at least 8 characters."));
  }

  private void signup(String email) throws Exception {
    mvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.signup("Ada Lovelace", email, "password12"))))
        .andExpect(status().isCreated());
  }
}
