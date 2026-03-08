import pandas as pd
from sklearn.ensemble import IsolationForest

# 1. Load your filled data
df = pd.read_csv('workout-generator//Synthetic_Climbing_Data.csv')

# 2. Select numerical columns for validation
num_cols = df.select_dtypes(include=['number']).columns

# 3. Train Isolation Forest
iso = IsolationForest(contamination=0.05, random_state=42)
df['is_outlier'] = iso.fit_predict(df[num_cols])

# --- NEW FUNCTIONALITY START ---

# 4. Create the Cleaned DataFrame
# We only keep rows where is_outlier is 1 (normal data)
df_cleaned = df[df['is_outlier'] == 1].copy()

# Optional: Drop the 'is_outlier' column so the file is "clean"
df_cleaned = df_cleaned.drop(columns=['is_outlier'])

# 5. Save the Cleaned data
df_cleaned.to_csv('Cleaned_Synthetic_AssessmentData.csv', index=False)

# --- NEW FUNCTIONALITY END ---

# 6. Keep your original validation export if you still want to see the flags
#df.to_csv('Validated_Synthetic_AssessmentData.csv', index=False)

outliers = df[df['is_outlier'] == -1]
print(f"Detected {len(outliers)} suspicious rows.")
print(f"Cleaned data saved with {len(df_cleaned)} rows.")