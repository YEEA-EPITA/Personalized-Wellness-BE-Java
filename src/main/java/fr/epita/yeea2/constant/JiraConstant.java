package fr.epita.yeea2.constant;

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
    }
}
