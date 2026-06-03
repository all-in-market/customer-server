#!/usr/bin/env bash
FILE=$(jq -r '.tool_input.file_path // ""')
if echo "$FILE" | grep -qE 'Service\.java$'; then
    BASENAME=$(basename "$FILE" .java)
    python3 -c "
import json, sys
f, bn = sys.argv[1], sys.argv[2]
print(json.dumps({
    'hookSpecificOutput': {
        'hookEventName': 'PostToolUse',
        'additionalContext': f'[Hook] {f} 을 수정했습니다. 대응하는 테스트 파일 {bn}Test.java 도 업데이트가 필요한지 확인해 주세요.'
    }
}))
" "$FILE" "$BASENAME"
fi
