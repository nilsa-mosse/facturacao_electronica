import re

file_path = "d:/eclipse_workspace/facturacao_electronica/src/main/resources/templates/menu.html"

with open(file_path, "r", encoding="utf-8") as f:
    html = f.read()

# Pattern captures all p tags containing th:text that also contain <i class="right...">
# We put the text inside a <span> and preserve the <i> tag
pattern = re.compile(r'<p\s+th:text="([^"]+)">([^<]*)<i\s+class="([^"]*right[^"]*)"([^>]*)></i>\s*</p>')

def repl(m):
    th_text_val = m.group(1)
    content_before_i = m.group(2)
    i_class = m.group(3)
    i_other = m.group(4)
    return f'<p><span th:text="{th_text_val}">{content_before_i}</span><i class="{i_class}"{i_other}></i></p>'

new_html, count1 = pattern.subn(repl, html)

# Some might have whitespace/newlines between p and i
pattern2 = re.compile(r'<p\s+th:text="([^"]+)">([^<]*)<i\s+class="([^"]*right[^"]*)"([^>]*)></i>([\s\S]*?)</p>')
def repl2(m):
    th_text_val = m.group(1)
    content_before_i = m.group(2)
    i_class = m.group(3)
    i_other = m.group(4)
    content_after_i = m.group(5)
    return f'<p><span th:text="{th_text_val}">{content_before_i}</span><i class="{i_class}"{i_other}></i>{content_after_i}</p>'

new_html, count2 = pattern2.subn(repl2, new_html)

total = count1 + count2

if total > 0:
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(new_html)
    print(f"Fixed {total} occurrences.")
else:
    print("No matches found.")
