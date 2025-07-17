package fr.epita.yeea2.constant;

public class PlatformConstant {
    public static final String JIRA = "JIRA";
    public static final String TRELLO = "TRELLO";

    public class JiraConstant {

        public enum IssueType {
            Task;

            @Override
            public String toString() {
                return name();
            }
        }

        public enum IssueStatus {
            Done;

            @Override
            public String toString() {
                return name();
            }
        }

        public static class JiraField {
            public static final String DUEDATE = "duedate";
            public static final String CREATED = "created";
            public static final String SUMMARY = "summary";
            public static final String STATUS = "status";
            public static final String TYPE = "type";
            public static final String ASSIGNEE = "assignee";
            public static final String PROJECT = "project";
            public static final String DESCRIPTION = "description";
            public static final String UPDATED = "updated";
            public static final String ISSUE = "issue";
            public static final String ID = "id";
            public static final String KEY = "key";
            public static final String CREATOR = "creator";
            public static final String DISPLAY_NAME = "displayName";
            public static final String FIELDS = "fields";
            public static final String ISSUE_TYPE = "issuetype";
            public static final String NAME = "name";
            public static final String EMAIL = "emailAddress";
            public static final String IMG = "avatarUrls";
        }
    }

    public class TrelloConstant {
        public static final String BASE_API_URL = "https://api.trello.com/1";
        public static final String MEMBER_ME = BASE_API_URL + "/members/me";
        public static final String BOARD_LISTS = BASE_API_URL + "/boards/%s/lists";
        public static final String LIST_CARDS = BASE_API_URL + "/lists/%s/cards";
        public static final String LIST_CARDS_NO_QUERY = BASE_API_URL + "/cards";
        public static final String MEMBER_BOARDS = MEMBER_ME + "/boards";
        public static final String LIST_DETAIL = BASE_API_URL+ "/lists/%s";


    }

}
