class Solution {
    public int solution(int[] blocks) {
        int n = blocks.length;
        if (n <= 1) return 0;
        
        int maxDistance = 0;
        
        // Try each possible starting position
        for (int start = 0; start < n; start++) {
            // Find leftmost position reachable from start
            int leftmost = findLeftmost(blocks, start);
            // Find rightmost position reachable from start
            int rightmost = findRightmost(blocks, start);
            
            // Calculate distance and update maximum
            int distance = rightmost - leftmost;
            maxDistance = Math.max(maxDistance, distance);
        }
        
        return maxDistance;
    }
    
    private int findLeftmost(int[] blocks, int start) {
        int pos = start;
        
        // Keep moving left while possible
        while (pos > 0) {
            // Can move to adjacent block or block with same/greater height
            if (blocks[pos - 1] >= blocks[start]) {
                pos--;
            } else {
                break;
            }
        }
        
        return pos;
    }
    
    private int findRightmost(int[] blocks, int start) {
        int pos = start;
        
        // Keep moving right while possible
        while (pos < blocks.length - 1) {
            // Can move to adjacent block or block with same/greater height
            if (blocks[pos + 1] >= blocks[start]) {
                pos++;
            } else {
                break;
            }
        }
        
        return pos;
    }
}