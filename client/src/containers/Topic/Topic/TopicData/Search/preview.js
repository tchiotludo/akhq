import { fieldLabel, OPERATORS } from './searchModel';

const operatorLabel = id => (OPERATORS.find(o => o.id === id)?.label || id).toLowerCase();

export const conditionText = c =>
  `${fieldLabel(c.field)} ${operatorLabel(c.operator)} "${c.value}"`;

// Joins are evaluated strictly left to right, so the preview groups explicitly.
export const expressionPreview = expression => {
  if (!expression || !expression.groups || expression.groups.length === 0) {
    return '';
  }
  return expression.groups
    .map((group, gi) => {
      const body = group.conditions
        .map((c, ci) => (ci === 0 ? '' : ` ${c.join} `) + conditionText(c))
        .join('');
      const wrapped = group.conditions.length > 1 ? `(${body})` : body;
      return (gi === 0 ? '' : ` ${group.join} `) + wrapped;
    })
    .join('');
};
