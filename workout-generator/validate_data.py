import pandas as pd
from sklearn.ensemble import IsolationForest

# 1. Load your filled data
df = pd.read_csv('workout-generator//Filled_AssessmentDataSept2022.csv')

# 2. Select numerical columns for validation
num_cols = df.select_dtypes(include=['number']).columns

# 3. Train Isolation Forest
# 'contamination' is the estimated percentage of "bad" data you expect (e.g., 5%)
iso = IsolationForest(contamination=0.05, random_state=42)
df['is_outlier'] = iso.fit_predict(df[num_cols])

# -1 indicates an outlier, 1 indicates normal data
outliers = df[df['is_outlier'] == -1]

# 4. Save the results
df.to_csv('Validated_AssessmentData.csv', index=False)

print(f"Detected {len(outliers)} suspicious rows.")