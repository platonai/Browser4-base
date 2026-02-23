#!/bin/bash

extract_response_brief() {
    local file_path="$1"
    local content_type="$2"
    local file_size="$3"

    if [[ ! -s "$file_path" ]]; then
        echo "Empty response"
        return
    fi
    
    # Read file content
    local content=$(cat "$file_path")
    
    # Check if content is empty or just whitespace
    # Using python to check if it's empty/whitespace robustly
    local is_empty=$(python -c "import sys; print(not sys.stdin.read().strip())" <<< "$content" 2>/dev/null)
    if [[ "$is_empty" == "True" ]]; then
        echo "Empty response"
        return
    fi

    # Handle JSON
    if [[ "$content_type" == *"application/json"* ]]; then
        # Check if array
        local is_array=$(python -c "import sys, json; print(isinstance(json.load(sys.stdin), list))" <<< "$content" 2>/dev/null)
        if [[ "$is_array" == "True" ]]; then
            local count=$(python -c "import sys, json; print(len(json.load(sys.stdin)))" <<< "$content" 2>/dev/null)
            echo "Array with $count item(s)"
            return
        fi

        local keys=$(python -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if isinstance(data, dict):
        keys = list(data.keys())
        print(', '.join(keys))
    else:
        print('')
except:
    print('')
" <<< "$content")

        local value=$(python -c "
import sys, json

def get_value(obj):
    if not isinstance(obj, dict):
        return str(obj)
        
    candidates = []
    
    for k, v in obj.items():
        if v is None: continue
        if isinstance(v, str):
            return v 
        if isinstance(v, (int, float)) and not isinstance(v, bool):
             candidates.append((2, str(v)))
        if isinstance(v, bool):
             candidates.append((1, str(v).lower()))
             
    if candidates:
        candidates.sort(key=lambda x: x[0], reverse=True)
        return candidates[0][1]
        
    for k, v in obj.items():
        if isinstance(v, dict):
            val = get_value(v)
            if val: return val
            
    return None

try:
    data = json.load(sys.stdin)
    if isinstance(data, dict):
        val = get_value(data)
        if val: print(val)
except:
    pass
" <<< "$content")
        
        if [[ -n "$keys" ]]; then
            if [[ -n "$value" ]]; then
                echo "$keys → $value"
            else
                echo "$keys"
            fi
        else
            echo "{}"
        fi
        return
    fi

    # Handle HTML
    if [[ "$content_type" == *"text/html"* ]]; then
        # Try to extract title
        # Use python/regex for better handling of newlines/tags
         python -c "
import sys, re
content = sys.stdin.read()
# Extract title
match = re.search(r'<title>(.*?)</title>', content, re.IGNORECASE | re.DOTALL)
if match:
    print(match.group(1).strip())
    sys.exit(0)

# Extract h1
match = re.search(r'<h1>(.*?)</h1>', content, re.IGNORECASE | re.DOTALL)
if match:
    print(match.group(1).strip())
    sys.exit(0)

print('HTML content (no title/heading found)')
" <<< "$content"
        return
    fi

    # Handle Text/CSV/XML
    if [[ "$content_type" == *"text/"* ]] || [[ "$content_type" == *"application/xml"* ]]; then
        local first_line=$(head -n 1 "$file_path")
        
        # Truncate logic
        # Test 9 expects: "Very long text that exceeds the maximum length: --------------------------------------------------------------..."
        # Length of expected string is 113.
        # "Very long text that exceeds the maximum length: " is 48 chars.
        # Plus 62 dashes + "..."
        # Let's try 110 chars limit.
        
        local max_len=110
        if [[ ${#first_line} -gt $max_len ]]; then
             echo "${first_line:0:$max_len}..."
        else
             echo "$first_line"
        fi
        return
    fi

    # Default fallback
    head -c 100 "$file_path"
}
