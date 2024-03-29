package server;

import java.io.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AuthManager {
    private JSONArray userData;

    /**
     * AuthManager Constructor
     * @throws Exception
     */
    public AuthManager() throws Exception {
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("accounts.txt")));

        // Read the json file
        String text = "";
        String line = null;
        while((line = fileReader.readLine()) != null) {
            text = text + line;
        }
        fileReader.close();

        JSONObject obj = new JSONObject(text);
        userData = obj.getJSONArray("users");
    }

    /**
     * Validates User ID
     * @param userID: int
     * @return status: int
     */
    public int validateUserID(int userID) {
        for (int i = 0; i< userData.length(); i++) {
            try {
                if (userData.getJSONObject(i).getInt("user-id") == userID) {
                    if ((userData.getJSONObject(i).getJSONArray("accounts").length() == 0) && (!userData.getJSONObject(i).has("password"))) {
                        return 2; // Logged In
                    } else {
                        return 1; // Not Logged In
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return 0; // Not Found
    }

    /**
     * Validates Accounts
     * @param userID: int
     * @param accountName: String
     * @return status: int
     */
    public int validateAccount(int userID, String accountName) {
        for (int i = 0; i< userData.length(); i++) {
            try {
                if (userData.getJSONObject(i).getInt("user-id") == userID) {
                    if (userData.getJSONObject(i).getJSONArray("accounts").length() == 0) {
                        return 1; // Account not required, needs password
                    }

                    for (int j = 0; j< userData.getJSONObject(i).getJSONArray("accounts").length(); j++) {
                        if (userData.getJSONObject(i).getJSONArray("accounts").getJSONObject(j).getString("account").equals(accountName)) {
                            if (userData.getJSONObject(i).has("password")) {
                                return 1; // Account good, needs password
                            } else {
                                return 2; // Logged In, password not required
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return 0; // Not Found
    }

    /**
     * Validates passwords
     * @param userID: int
     * @param accountName: String
     * @param password: String
     * @return status: int
     */
    public int validatePassword(int userID, String accountName, String password) {
        for (int i = 0; i< userData.length(); i++) {
            try {
                if (userData.getJSONObject(i).getInt("user-id") == userID) {
                    if (userData.getJSONObject(i).has("password")) {
                        if (userData.getJSONObject(i).getString("password").equals(password)) {
                            if ((accountName == null) && (userData.getJSONObject(i).getJSONArray("accounts").length() != 0)) {
                                return 1; // Needs account name
                            } else {
                                return 2; // Logged in
                            }
                        } else {
                            return 0; // Wrong password
                        }
                    } else {
                        return 1; // No password, therefore account name required
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return 0; // Not Found
    }
}
