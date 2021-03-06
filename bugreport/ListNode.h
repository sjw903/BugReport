#ifndef _NsBabysitter_ListNode_h_
#define _NsBabysitter_ListNode_h_
#ifndef __cplusplus
#error "This is a C++ header file; it requires C++ to compile."
#endif
//--------------------------------------------------------------------------------------------------
// Description:
//  An abstract class which must be the base class for any objects to be managed and stored as a
//  List object.  It implements all operations needed by List for managing a doubly linked list.
//--------------------------------------------------------------------------------------------------

#include <stdlib.h>

namespace NsBabysitter {
class ListNode
{
public :
    ListNode();
    virtual ~ListNode();
    ListNode* getPtrNext()const {return mNext;}
    ListNode* getPtrPrev()const {return mPrev;}
    bool isFirst() const {return (NULL == mPrev);}
    bool isLast() const {return (NULL == mNext);}
    void setPtrNext(ListNode* next) {mNext = next;}
    void setPtrPrev(ListNode* prev) {mPrev = prev;}

protected :
    ListNode* mPrev;
    ListNode* mNext;
};
} // namespace
#endif // _NsBabysitter_List_h_
