package fr.epita.yeea2.constant;

public class EmailConstant {
    public static final String SYSTEM_NAME = "YEEA - Personalized Wellness Planner";
    public static String htmlEmailTemplate = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px; color: #333; }
                .container { background-color: #fff; border-radius: 6px; padding: 20px; max-width: 600px; margin: auto; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                h2 { color: #0055a5; }
                .highlight { font-weight: bold; color: #d9534f; }
                .section { margin-bottom: 15px; }
                .footer { font-size: 12px; color: #888; margin-top: 20px; text-align: center; }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>🧠 Daily Burnout Report</h2>
                <div class="section">
                    <strong>Date:</strong> %s<br>
                    <strong>User:</strong> %s
                </div>
                <div class="section">
                    <strong>Burnout Score:</strong> <span class="highlight">%d</span><br>
                    <strong>Risk Level:</strong> <span class="highlight">%s</span>
                </div>
                <div class="section">
                    <strong>Recommendation:</strong><br>
                    <em>%s</em>
                </div>
                <hr>
                <div class="section">
                    <strong>Additional Insights:</strong><br>
                    <ul>
                        <li>Extended Work Sessions: %d</li>
                        <li>Lack of Breaks: %d</li>
                        <li>Night Work: %d</li>
                        <li>Today's Workload: %d</li>
                        <li>Frequent Context Switching: %d</li>
                    </ul>
                </div>
                <div class="footer">
                    This report is auto-generated. For questions, contact your support team.
                </div>
            </div>
        </body>
        </html>
        """;

}
