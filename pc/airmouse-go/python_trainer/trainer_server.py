from flask import Flask, request, jsonify
from fine_tune import fine_tune_model
import pandas as pd
import os

app = Flask(__name__)

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok"})

@app.route('/fine_tune', methods=['POST'])
def fine_tune():
    data = request.get_json()
    model_path = data.get('model_path', 'models/base_model.pth')
    buffer = pd.DataFrame(data['buffer'])
    output_path = data.get('output_path', 'models/personalized_model.pth')
    new_model = fine_tune_model(model_path, buffer, output_path)
    return jsonify({"status": "success", "model_path": new_model})

if __name__ == '__main__':
    os.makedirs('models', exist_ok=True)
    app.run(host='127.0.0.1', port=5001)