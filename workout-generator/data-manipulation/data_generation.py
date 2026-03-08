import pandas as pd
from sdv.single_table import CTGANSynthesizer
from sdv.metadata import SingleTableMetadata

# 1. Load your cleaned data
# Note: Using the filename from your last step
df = pd.read_csv('workout-generator//_Cleaned_AssessmentData.csv')

# 2. Detect Metadata
# SDV automatically identifies which columns are numerical vs categorical
metadata = SingleTableMetadata()
metadata.detect_from_dataframe(df)

# 3. Initialize the CTGAN Synthesizer
# This is the "brain" that will learn from your climbing data
synthesizer = CTGANSynthesizer(
    metadata,
    enforce_rounding=False,
    epochs=500, # Increase this for better quality, decrease for speed
    verbose=True
)

# 4. Train the model
print("Training the AI model... this may take a few minutes.")
synthesizer.fit(df)

# 5. Generate new synthetic data
# Let's generate 500 new "fake" climbers
synthetic_data = synthesizer.sample(num_rows=10000)

# 6. Save the results
synthetic_data.to_csv('workout-generator//Synthetic_Climbing_Data.csv', index=False)

print(f"Successfully generated {len(synthetic_data)} synthetic climbing profiles!")