#ifndef _NsBabysitter_List_h_
#define _NsBabysitter_List_h_
#ifndef __cplusplus
#error "This is a C++ header file; it requires C++ to compile."
#endif
//--------------------------------------------------------------------------------------------------
// Description:
//  An implementation of a doubly-linked list.  It contains a list of objects derived from ListNode.
//--------------------------------------------------------------------------------------------------

#include <stdlib.h>
#include "ListNode.h"

namespace NsBabysitter {
class List
{
public:
    List();
    virtual ~List();
    unsigned int getNumNodes()const {return mNumNodes;}
    ListNode* getFirst()const {return mFirstNode;}
    ListNode* getLast()const;
    bool insertAfter(ListNode* currNode, ListNode* newNode);
    bool insertBefore(ListNode* currNode, ListNode* newNode);
    bool insertFirst(ListNode* newNode) {return insertBefore(mFirstNode, newNode);}
    bool insertLast(ListNode* newNode) {return insertAfter(getLast(), newNode);}
    bool isEmpty()const {return (NULL == mFirstNode);}
    void enqueue(ListNode* newNode) {insertBefore(mFirstNode, newNode);}
    ListNode* dequeue();
    void remove(ListNode* node);
    void removeAllAndDelete();
    void removeAndDelete(ListNode* node);

private:
    unsigned int    mNumNodes;
    ListNode*       mFirstNode;
};
} // namespace
#endif // _NsBabysitter_List_h_
