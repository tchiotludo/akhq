import { describe, expect, it } from 'vitest';
import { TopicGroups } from './TopicGroups';

describe('TopicGroups theme styles', () => {
  it('uses theme-aware styles for topic and lag pills', () => {
    const topicGroups = new TopicGroups({});
    topicGroups.state = { selectedCluster: 'cluster-a' };

    const [topicLink] = topicGroups.handleTopics({ 'topic-a': 12 });
    const lagBadge = topicLink.props.children.at(-1);

    expect(topicLink.props.className).toContain('btn-theme-secondary');
    expect(lagBadge.props.className).toContain('badge-theme-secondary');
  });
});
