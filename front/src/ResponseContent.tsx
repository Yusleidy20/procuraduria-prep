interface ResponseContentProps {
  text: string;
}

export function ResponseContent({ text }: ResponseContentProps) {
  const blocks = text
    .split(/\n\s*\n/)
    .map((block) => block.trim())
    .filter(Boolean);

  return (
    <div className="response-content">
      {blocks.map((block, index) => {
        const lines = block.split("\n").map((line) => line.trim()).filter(Boolean);
        const isList = lines.length > 0 && lines.every((line) => /^([-*]|\d+[.)])\s+/.test(line));

        if (isList) {
          const isOrderedList = /^\d+[.)]\s+/.test(lines[0]);
          const items = lines.map((line, lineIndex) => (
            <li key={lineIndex}>{line.replace(/^([-*]|\d+[.)])\s+/, "")}</li>
          ));
          return isOrderedList ? <ol key={index}>{items}</ol> : <ul key={index}>{items}</ul>;
        }

        return <p key={index}>{block}</p>;
      })}
    </div>
  );
}
